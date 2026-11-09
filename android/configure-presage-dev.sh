#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO="$HOME/smart-spectra-hormigas-edge"
PROJECT="$REPO/android"
GRADLE_FILE="$PROJECT/app/build.gradle.kts"
SDK="$HOME/android-sdk"
CONFIG_DIR="$HOME/.config/smart-spectra-challenge"
KEY_FILE="$CONFIG_DIR/presage.properties"
STAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP="$GRADLE_FILE.bak-presage-dev-$STAMP"
LOG="$HOME/smartspectra-presage-dev-build-$STAMP.log"

if [ ! -f "$GRADLE_FILE" ] || [ ! -f "$PROJECT/settings.gradle.kts" ]; then
  echo "No se encontraron los archivos Gradle Android requeridos."
  exit 2
fi

if [ ! -d "$SDK/platforms/android-36" ]; then
  echo "No encuentro Android SDK Platform 36 en $SDK."
  exit 2
fi

if [ -e "$KEY_FILE" ]; then
  echo "Ya existe $KEY_FILE."
  echo "No se sobrescribió. No ejecutes el script hasta decidir cómo conservar/reemplazarla."
  exit 2
fi

if grep -q 'buildConfigField("String", "PRESAGE_API_KEY"' "$GRADLE_FILE"; then
  echo "El campo PRESAGE_API_KEY ya está declarado; no se modificó nada."
  exit 2
fi

echo "Este flujo es para desarrollo local/QA según la guía Android de Presage."
echo "La clave se guardará fuera del repo, pero quedará incorporada al APK."
echo "No distribuyas ni publiques ese APK."
read -r -p 'Para continuar, escribe SOLO-APK-LOCAL: ' CONFIRM
if [ "$CONFIRM" != "SOLO-APK-LOCAL" ]; then
  echo "Cancelado. No se pidió ni guardó ninguna clave."
  exit 0
fi

printf 'Pega tu API key de desarrollo de Presage (entrada oculta): '
IFS= read -r -s API_KEY
printf '\n'

if [ -z "$API_KEY" ]; then
  echo "Clave vacía; cancelado sin modificar archivos."
  exit 2
fi
case "$API_KEY" in
  *$'\n'*|*$'\r'*)
    echo "La clave contiene saltos de línea; cancelado."
    exit 2
    ;;
esac

mkdir -p "$CONFIG_DIR"
chmod 700 "$CONFIG_DIR"
cp -p "$GRADLE_FILE" "$BACKUP"
chmod 600 "$BACKUP"

export PRESAGE_DEV_KEY="$API_KEY"
python3 - "$KEY_FILE" "$GRADLE_FILE" <<'PY'
import os
import pathlib
import re
import sys

key_path = pathlib.Path(sys.argv[1])
gradle_path = pathlib.Path(sys.argv[2])
key = os.environ.get("PRESAGE_DEV_KEY", "")

if not key:
    raise SystemExit("No se recibió la clave por el canal local oculto.")

# Escapar caracteres para el formato Java .properties.
escaped = (
    key.replace("\\", "\\\\")
       .replace("\t", "\\t")
       .replace("\f", "\\f")
       .replace("=", "\\=")
       .replace(":", "\\:")
       .replace(" ", "\\ ")
       .replace("#", "\\#")
       .replace("!", "\\!")
)
key_path.write_text("PRESAGE_API_KEY=" + escaped + "\n", encoding="utf-8")
key_path.chmod(0o600)

text = gradle_path.read_text(encoding="utf-8")

if "PRESAGE_API_KEY" in text:
    raise SystemExit("El Gradle ya menciona PRESAGE_API_KEY; se aborta para evitar duplicados.")

if not re.search(r'(?m)^plugins\s*\{', text):
    raise SystemExit("No se encontró el bloque plugins esperado; Gradle no se parcheó.")

# Importaciones del DSL Kotlin, colocadas antes de plugins.
imports = "import java.io.File\nimport java.util.Properties\n"
if "import java.util.Properties" not in text:
    text = imports + text

android_anchor = re.search(r'(?m)^android\s*\{', text)
if not android_anchor:
    raise SystemExit("No se encontró android {; Gradle no se parcheó.")

loader = '''
// Credencial para builds locales de desarrollo. El valor se incluye en el APK;
// no usar este mecanismo para distribuir builds.
val presageLocalFile = File(
    System.getProperty("user.home"),
    ".config/smart-spectra-challenge/presage.properties"
)
val presageLocalProperties = Properties().apply {
    if (presageLocalFile.isFile) {
        presageLocalFile.inputStream().use { load(it) }
    }
}
val presageLocalKey = presageLocalProperties
    .getProperty("PRESAGE_API_KEY", "")
    .replace("\\\\", "\\\\\\\\")
    .replace("\\"", "\\\\\\"")
    .replace("\\n", "")
'''

text = text[:android_anchor.start()] + loader + "\n" + text[android_anchor.start():]

default_config = re.search(r'(?m)^(\s*)defaultConfig\s*\{', text)
if not default_config:
    raise SystemExit("No se encontró defaultConfig {; Gradle no se parcheó.")

indent = default_config.group(1) + "    "
insertion_at = default_config.end()
field = (
    "\n"
    + indent
    + 'buildConfigField("String", "PRESAGE_API_KEY", "\\"$presageLocalKey\\"")'
)
text = text[:insertion_at] + field + text[insertion_at:]

if not re.search(r'buildFeatures\s*\{[^}]*buildConfig\s*=\s*true', text, re.S):
    android_block = re.search(r'(?m)^android\s*\{', text)
    if not android_block:
        raise SystemExit("No se pudo localizar android { para habilitar BuildConfig.")
    insertion_at = android_block.end()
    text = (
        text[:insertion_at]
        + "\n    buildFeatures {\n        buildConfig = true\n    }\n"
        + text[insertion_at:]
    )

gradle_path.write_text(text, encoding="utf-8")
print("Configuración local preparada. El valor no se imprime.")
PY

unset API_KEY PRESAGE_DEV_KEY
chmod 600 "$KEY_FILE"

echo
echo "Credencial guardada fuera del repositorio con permisos privados."
echo "ATENCIÓN: al compilar, la clave queda incorporada al APK."
echo "Respaldo de Gradle: $BACKUP"

export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"

echo
echo "=== Compilar APK debug y ejecutar lint ==="
echo "No se instalará la app ni se iniciará la cámara."
set +e
studio build "$PROJECT" :app:assembleDebug :app:lintDebug 2>&1 | tee "$LOG"
RESULT=${PIPESTATUS[0]}
set -e

if [ "$RESULT" -ne 0 ]; then
  echo "Build/lint falló (código $RESULT)."
  echo "Registro: $LOG"
  echo "Respaldo: $BACKUP"
  exit "$RESULT"
fi

APK="$PROJECT/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -s "$APK" ]; then
  echo "Build terminó sin encontrar el APK esperado: $APK"
  exit 3
fi

echo
echo "Build completada. APK de desarrollo:"
ls -lh "$APK"
sha256sum "$APK"
echo "Registro: $LOG"
echo "No compartas el APK: contiene la API key de desarrollo."
echo "La app no se instaló y la cámara no se inició."
