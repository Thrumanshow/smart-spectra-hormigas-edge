#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO="$HOME/smart-spectra-hormigas-edge"
PROJECT="$REPO/android"
MANIFEST="$PROJECT/app/src/main/AndroidManifest.xml"
SDK="$HOME/android-sdk"
STAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP="$MANIFEST.bak.$STAMP"
LOG="$HOME/smart-spectra-lint-$STAMP.log"

cd "$REPO"

if ! grep -q 'android.permission.CAMERA' "$MANIFEST"; then
  echo "No encuentro el permiso de cámara; no se modificó nada."
  exit 2
fi

if grep -q 'android.hardware.camera' "$MANIFEST"; then
  echo "Ya existe una declaración android.hardware.camera."
  echo "No se modificó el manifiesto. Revísalo manualmente:"
  grep -n 'android.hardware.camera' "$MANIFEST"
  exit 2
fi

cp -p "$MANIFEST" "$BACKUP"
echo "Copia de seguridad: $BACKUP"

python3 - "$MANIFEST" <<'PY'
import pathlib
import sys

path = pathlib.Path(sys.argv[1])
text = path.read_text(encoding="utf-8")
camera_permission = '    <uses-permission android:name="android.permission.CAMERA" />\n'
feature = (
    '    <uses-feature android:name="android.hardware.camera" '
    'android:required="false" />\n'
)

if text.count(camera_permission) != 1:
    raise SystemExit(
        "No encontré exactamente una línea de permiso de cámara con el formato esperado; "
        "no se aplicó el parche."
    )

if "android.hardware.camera" in text:
    raise SystemExit("Ya existe una declaración de hardware de cámara; no se aplicó el parche.")

path.write_text(text.replace(camera_permission, camera_permission + feature), encoding="utf-8")
print("Añadida la declaración de cámara opcional requerida por lint.")
PY

echo
echo "=== Validar el manifiesto XML ==="
python3 - "$MANIFEST" <<'PY'
import sys
import xml.etree.ElementTree as ET

ET.parse(sys.argv[1])
print("XML válido.")
PY

export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"

echo
echo "=== Compilar APK debug y ejecutar lint ==="
echo "No se instala la app ni se inicia la cámara."
set +e
studio build "$PROJECT" :app:assembleDebug :app:lintDebug 2>&1 | tee "$LOG"
RESULT=${PIPESTATUS[0]}
set -e

if [ "$RESULT" -ne 0 ]; then
  echo
  echo "Build/lint falló con código $RESULT."
  echo "Registro: $LOG"
  echo "Manifiesto original respaldado en: $BACKUP"
  exit "$RESULT"
fi

APK="$PROJECT/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -s "$APK" ]; then
  echo "Gradle terminó, pero no aparece el APK esperado: $APK"
  echo "Registro: $LOG"
  exit 3
fi

echo
echo "Build y lint completados."
ls -lh "$APK"
echo "Registro: $LOG"
echo "La app no se instaló; no hubo acceso a la cámara."
