#!/data/data/com.termux/files/usr/bin/bash
set -eu

ROOT="$(cd "$(dirname "$0")" && pwd)"
SDK="$HOME/android-sdk"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"

if ! command -v studio >/dev/null 2>&1; then
  echo "FALTA el comando studio. No se instaló nada."
  exit 2
fi

if [ ! -d "$SDK/platforms/android-36" ]; then
  echo "FALTA Android Platform 36 en $SDK"
  exit 2
fi

export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"

echo "=== Diagnóstico de toolchain ==="
studio doctor

echo
echo "=== Compilar APK debug y ejecutar lint ==="
echo "Esto puede descargar dependencias Gradle. No instala ni lanza la app."
studio build "$ROOT" :app:assembleDebug :app:lintDebug

if [ ! -s "$APK" ]; then
  echo "ERROR: no se encontró el APK esperado: $APK"
  exit 3
fi

echo
echo "=== APK generado ==="
ls -lh "$APK"

if [ -x "$SDK/build-tools/36.0.0/apksigner" ]; then
  "$SDK/build-tools/36.0.0/apksigner" verify --verbose "$APK"
elif command -v apksigner >/dev/null 2>&1; then
  apksigner verify --verbose "$APK"
else
  echo "Aviso: apksigner no está disponible; no se verificó la firma."
fi

echo
echo "Build finalizada. No se instaló el APK y no se activó la cámara."
