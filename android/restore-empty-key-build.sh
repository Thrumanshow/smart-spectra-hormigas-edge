#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO="$HOME/smart-spectra-hormigas-edge"
PROJECT="$REPO/android"
GRADLE_FILE="$PROJECT/app/build.gradle.kts"
KOTLIN_FILE="$PROJECT/app/src/main/java/com/hormigasais/presagechallenge/MainActivity.kt"
SDK="$HOME/android-sdk"
STAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP="$GRADLE_FILE.bak-empty-key-$STAMP"
LOG="$HOME/smartspectra-empty-key-build-$STAMP.log"

if [ ! -f "$GRADLE_FILE" ] || [ ! -f "$KOTLIN_FILE" ]; then
  echo "Faltan archivos Android necesarios; no se hizo ningún cambio."
  exit 2
fi

if grep -q 'PRESAGE_API_KEY' "$GRADLE_FILE"; then
  echo "Gradle ya contiene PRESAGE_API_KEY; no se modificó."
  exit 2
fi

if ! grep -q 'BuildConfig\.PRESAGE_API_KEY' "$KOTLIN_FILE"; then
  echo "Kotlin ya no referencia BuildConfig.PRESAGE_API_KEY."
  echo "No se añadió ninguna configuración."
  exit 2
fi

if ! grep -q 'versionName = "0.1.0"' "$GRADLE_FILE"; then
  echo 'No hallé el ancla esperada versionName = "0.1.0". No se modificó.'
  exit 2
fi

cp -p "$GRADLE_FILE" "$BACKUP"
echo "Respaldo: $BACKUP"

python3 - "$GRADLE_FILE" <<'PY'
import pathlib
import sys

p = pathlib.Path(sys.argv[1])
s = p.read_text(encoding="utf-8")
anchor = '        versionName = "0.1.0"'
if s.count(anchor) != 1:
    raise SystemExit("Se esperaba exactamente una línea versionName; se aborta.")

replacement = (
    anchor
    + '\n        // Vacío deliberadamente: no colocar una clave privada dentro del APK.'
    + '\n        buildConfigField("String", "PRESAGE_API_KEY", "\\"\\\"")'
)
# La expresión Kotlin resultante debe ser: buildConfigField("String", "PRESAGE_API_KEY", "\"\"")
replacement = (
    anchor
    + '\n        // Vacío deliberadamente: no colocar una clave privada dentro del APK.'
    + '\n        buildConfigField("String", "PRESAGE_API_KEY", "\\"\\u0022")'
)
p.write_text(s.replace(anchor, replacement), encoding="utf-8")
print("Añadida una constante BuildConfig deliberadamente vacía.")
PY

echo
echo "--- Confirmar configuración sin revelar credenciales ---"
grep -n 'PRESAGE_API_KEY' "$GRADLE_FILE" "$KOTLIN_FILE" || true

export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"

echo
echo "--- Compilar debug y ejecutar lint; no instalar ni iniciar la cámara ---"
set +e
studio build "$PROJECT" :app:assembleDebug :app:lintDebug 2>&1 | tee "$LOG"
RESULT=${PIPESTATUS[0]}
set -e

if [ "$RESULT" -ne 0 ]; then
  echo "Build/lint falló (código $RESULT)."
  echo "Log: $LOG"
  echo "Respaldo Gradle: $BACKUP"
  exit "$RESULT"
fi

APK="$PROJECT/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -s "$APK" ]; then
  echo "No se encontró el APK esperado: $APK"
  exit 3
fi

echo
echo "Compilación terminada. APK:"
ls -lh "$APK"
echo "Log: $LOG"
echo "La credencial está vacía; la sesión SmartSpectra no debe iniciarse."
