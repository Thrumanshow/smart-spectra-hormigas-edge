#!/data/data/com.termux/files/usr/bin/bash
set -eu

REPO="$HOME/smart-spectra-hormigas-edge"
APK="$REPO/android/app/build/outputs/apk/debug/app-debug.apk"
SIGNER="$HOME/android-sdk/build-tools/36.0.0/apksigner"
KEYFILE="$HOME/.config/smart-spectra-challenge/presage.properties"

if [ ! -s "$APK" ]; then
  echo "No encuentro un APK válido:"
  echo "$APK"
  exit 2
fi

if [ ! -x "$SIGNER" ]; then
  echo "No encuentro apksigner ejecutable:"
  echo "$SIGNER"
  exit 3
fi

echo "=== APK ==="
ls -lh "$APK"
sha256sum "$APK"

echo
echo "=== Verificación de firma ==="
"$SIGNER" verify --verbose "$APK"

echo
echo "=== Preparación de la prueba ==="
if [ -f "$KEYFILE" ] &&
   grep -q '^[[:space:]]*PRESAGE_API_KEY[[:space:]]*=[[:space:]]*[^[:space:]]' "$KEYFILE"; then
  echo "Se detecta una clave local configurada; su valor no se mostrará."
  if [ "$KEYFILE" -nt "$APK" ]; then
    echo "ATENCIÓN: la clave es más reciente que el APK."
    echo "Este APK no se ha recompilado desde que se modificó esa configuración."
  else
    echo "No compartas el APK: BuildConfig puede incorporar la clave."
  fi
else
  echo "No se detecta una clave local no vacía."
  echo "La instalación puede comprobar el consentimiento, pero no confirma señal en vivo."
fi

if ! command -v termux-open >/dev/null 2>&1; then
  echo
  echo "No está disponible termux-open; no se abrió el instalador."
  exit 4
fi

echo
echo "Se abrirá el instalador de Android para que confirmes o canceles."
echo "Este script no concede permisos ni inicia la cámara."
termux-open "$APK"

echo
echo "Si Android completa la instalación, abre la app manualmente."
echo "Lee el aviso y acepta el consentimiento solo si estás de acuerdo."
echo "El permiso de cámara se solicitará después en Android."
