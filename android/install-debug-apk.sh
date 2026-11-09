#!/data/data/com.termux/files/usr/bin/bash
set -eu

REPO="$HOME/smart-spectra-hormigas-edge"
APK="$REPO/android/app/build/outputs/apk/debug/app-debug.apk"
SDK="$HOME/android-sdk"
SIGNER="$SDK/build-tools/36.0.0/apksigner"
AAPT="$SDK/build-tools/36.0.0/aapt"
KEYFILE="$HOME/.config/smart-spectra-challenge/presage.properties"

if [ ! -s "$APK" ]; then
  echo "No encuentro un APK válido en:"
  echo "$APK"
  exit 2
fi

echo "APK:"
ls -lh "$APK"
echo
echo "SHA-256:"
sha256sum "$APK"

if [ -x "$SIGNER" ]; then
  echo
  echo "Verificación de firma:"
  "$SIGNER" verify --verbose "$APK"
else
  echo "DETENIDO: no encuentro apksigner para verificar el APK."
  exit 3
fi

if [ -x "$AAPT" ]; then
  echo
  echo "Identidad de la app:"
  "$AAPT" dump badging "$APK" | grep -m1 "^package:"
else
  echo "Aviso: aapt no está disponible para leer el identificador del paquete."
fi

echo
if [ -f "$KEYFILE" ] &&
   grep -q '^[[:space:]]*PRESAGE_API_KEY[[:space:]]*=[[:space:]]*[^[:space:]]' "$KEYFILE"; then
  echo "Hay una API key configurada localmente."
  echo "No se muestra su valor. No compartas este APK: BuildConfig puede empaquetarla."
  if [ "$KEYFILE" -nt "$APK" ]; then
    echo "ATENCIÓN: la configuración de la clave es más reciente que el APK."
    echo "El APK podría no contenerla; recompila antes de probar señales en vivo."
  fi
else
  echo "No se detecta una API key local no vacía."
  echo "La app puede mostrar el consentimiento, pero no iniciará una sesión SmartSpectra funcional."
fi

if ! command -v termux-open >/dev/null 2>&1; then
  echo
  echo "DETENIDO: no está disponible termux-open para abrir el instalador Android."
  exit 4
fi

echo
echo "Abriendo el instalador del sistema Android."
echo "La instalación requiere tu confirmación en pantalla; cámara y permisos no se conceden aquí."
termux-open "$APK"
