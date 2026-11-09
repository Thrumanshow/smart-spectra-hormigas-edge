#!/data/data/com.termux/files/usr/bin/bash
set -eu

ROOT="$(cd "$(dirname "$0")" && pwd)"
APP="$ROOT/app"
PROJECT="$(cd "$ROOT/.." && pwd)"

required_files=(
  "$ROOT/settings.gradle.kts"
  "$ROOT/build.gradle.kts"
  "$ROOT/gradle.properties"
  "$APP/build.gradle.kts"
  "$APP/src/main/AndroidManifest.xml"
  "$APP/src/main/java/com/hormigasais/presagechallenge/MainActivity.kt"
)

echo "=== 1. Verificar que existan los archivos ==="
for f in "${required_files[@]}"; do
  if [ ! -s "$f" ]; then
    echo "FALTA o está vacío: $f"
    exit 2
  fi
  echo "OK: ${f#"$PROJECT"/}"
done

echo
echo "=== 2. Detectar posibles residuos de pegado de Termux ==="
for f in \
  "$ROOT/settings.gradle.kts" \
  "$ROOT/build.gradle.kts" \
  "$APP/build.gradle.kts" \
  "$APP/src/main/AndroidManifest.xml" \
  "$APP/src/main/java/com/hormigasais/presagechallenge/MainActivity.kt"
do
  if grep -nE '^[[:space:]]*(🐜|> |cat > |EOF[[:space:]]*$|No command )' "$f"; then
    echo "ERROR: parece haber prompts o líneas de terminal pegadas dentro de $f"
    echo "No se intentará compilar. Corrige el archivo antes."
    exit 3
  fi
done
echo "No se detectaron líneas de prompt típicas."

echo
echo "=== 3. Validar manifiesto XML ==="
python3 - "$APP/src/main/AndroidManifest.xml" <<'PY'
import sys
import xml.etree.ElementTree as ET

path = sys.argv[1]
try:
    root = ET.parse(path).getroot()
except Exception as exc:
    print(f"XML inválido: {exc}")
    raise SystemExit(1)

ns = "{http://schemas.android.com/apk/res/android}"
permissions = {
    item.get(f"{ns}name")
    for item in root.findall("uses-permission")
}
needed = {
    "android.permission.CAMERA",
    "android.permission.INTERNET",
}
missing = needed - permissions
if missing:
    print("Faltan permisos declarados:", ", ".join(sorted(missing)))
    raise SystemExit(1)

print("Manifiesto XML válido; declara cámara e Internet.")
PY

echo
echo "=== 4. Revisar toolchain disponible, sin instalar nada ==="
if ! command -v java >/dev/null 2>&1; then
  echo "FALTA Java."
  exit 4
fi
java -version 2>&1 | head -n 1

if ! command -v gradle >/dev/null 2>&1; then
  echo "FALTA Gradle en PATH. No se instalará automáticamente."
  exit 4
fi
gradle --version | head -n 8

SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$SDK" ]; then
  echo "Falta ANDROID_SDK_ROOT o ANDROID_HOME. No se instalará el SDK."
  exit 4
fi
if [ ! -d "$SDK" ]; then
  echo "La ruta del Android SDK no existe: $SDK"
  exit 4
fi
echo "Android SDK: $SDK"

if [ ! -d "$SDK/platforms/android-36" ] &&
   [ ! -d "$SDK/platforms/android-36.1" ]; then
  echo "No encuentro Android platform 36 o 36.1 en el SDK."
  echo "No se intentará compilar."
  exit 4
fi

echo
echo "=== 5. Comprobación estática de API y consentimiento ==="
KOTLIN="$APP/src/main/java/com/hormigasais/presagechallenge/MainActivity.kt"

for token in \
  "SmartSpectraSdk.shared" \
  "sdk.config.apiKey" \
  "sdk.config.imageOutputEnabled = false" \
  "Manifest.permission.CAMERA" \
  "showConsent()" \
  "sdk.start()" \
  "sdk.stop()"
do
  if ! grep -Fq "$token" "$KOTLIN"; then
    echo "Falta en MainActivity.kt: $token"
    exit 5
  fi
done

if grep -Fq "androidx.appcompat.app.AlertDialog" "$KOTLIN"; then
  echo "ERROR: MainActivity usa AppCompat AlertDialog pero el scaffold no incluye esa dependencia."
  echo "Usa android.app.AlertDialog o añade una dependencia compatible."
  exit 5
fi
echo "Encontrados los componentes de SmartSpectra, consentimiento y control de inicio/parada."

echo
echo "=== 6. Compilar y ejecutar lint ==="
echo "Esto puede descargar dependencias Gradle/SmartSpectra al caché del usuario."
echo "No usa la API key, no instala la app y no inicia la cámara."

gradle --no-daemon --console=plain \
  -p "$ROOT" \
  :app:assembleDebug \
  :app:lintDebug

APK="$APP/build/outputs/apk/debug/app-debug.apk"
if [ ! -s "$APK" ]; then
  echo "Gradle terminó, pero no encuentro el APK esperado: $APK"
  exit 6
fi

echo
echo "=== Resultado ==="
ls -lh "$APK"

if command -v apksigner >/dev/null 2>&1; then
  apksigner verify --verbose "$APK"
else
  echo "Aviso: apksigner no está disponible; no se verificó la firma localmente."
fi

echo
echo "Build y lint completados. La cámara NO se inició."
echo "Este APK no tiene una API key de desarrollo salvo que ya se hubiera configurado localmente."
