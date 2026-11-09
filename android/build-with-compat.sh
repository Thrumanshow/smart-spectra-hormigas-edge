#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

REPO="$(cd "$(dirname "$0")/.." && pwd)"
PROJECT="$REPO/android"
SDK="$HOME/android-sdk"
COMPAT="$HOME/.studio/studio_compat.py"
PREPARED=0
TMP=""

restore_compat() {
  if [ "$PREPARED" -eq 1 ]; then
    echo
    echo "Restaurando los cambios temporales del adaptador…"
    python3 "$COMPAT" --sdk "$SDK" --restore "$PROJECT" || {
      echo "ATENCIÓN: restauración automática falló."
      echo "Ejecuta manualmente:"
      echo "python3 \"$COMPAT\" --sdk \"$SDK\" --restore \"$PROJECT\""
      return 1
    }
    PREPARED=0
  fi
}

cleanup() {
  rc=$?
  trap - EXIT INT TERM
  if [ "$PREPARED" -eq 1 ]; then
    restore_compat || rc=1
  fi
  if [ -n "$TMP" ] && [ -d "$TMP" ]; then
    rm -rf "$TMP"
  fi
  exit "$rc"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

if [ ! -f "$COMPAT" ]; then
  echo "No se encuentra el adaptador: $COMPAT"
  exit 2
fi
if [ ! -f "$PROJECT/settings.gradle.kts" ] ||
   [ ! -f "$PROJECT/build.gradle.kts" ] ||
   [ ! -f "$PROJECT/app/build.gradle.kts" ]; then
  echo "Faltan archivos esenciales del proyecto Android."
  exit 2
fi
if ! command -v studio >/dev/null 2>&1; then
  echo "No se encuentra el comando studio."
  exit 2
fi
if [ ! -d "$SDK/platforms/android-36" ]; then
  echo "No se encuentra Android Platform 36 en $SDK."
  exit 2
fi

export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"

TMP="$(mktemp -d "$HOME/.compat-review.XXXXXX")"
chmod 700 "$TMP"

FILES=(
  "settings.gradle.kts"
  "build.gradle.kts"
  "app/build.gradle.kts"
  "gradle.properties"
)

echo "=== Guardando referencia temporal de archivos Gradle ==="
for rel in "${FILES[@]}"; do
  if [ -f "$PROJECT/$rel" ]; then
    mkdir -p "$TMP/$(dirname "$rel")"
    cp -p "$PROJECT/$rel" "$TMP/$rel"
  fi
done

echo
echo "=== Aplicando preparación reversible para inspección ==="
python3 "$COMPAT" --sdk "$SDK" --prepare "$PROJECT"
PREPARED=1

echo
echo "=== Diferencias generadas por el adaptador ==="
CHANGED=0
for rel in "${FILES[@]}"; do
  if [ -f "$TMP/$rel" ] && [ -f "$PROJECT/$rel" ]; then
    if ! cmp -s "$TMP/$rel" "$PROJECT/$rel"; then
      CHANGED=1
      echo
      echo "--- $rel ---"
      diff -u "$TMP/$rel" "$PROJECT/$rel" || true
    fi
  elif [ -f "$PROJECT/$rel" ]; then
    CHANGED=1
    echo "Nuevo archivo generado: $rel"
    sed -n '1,160p' "$PROJECT/$rel"
  fi
done

if [ "$CHANGED" -eq 0 ]; then
  echo "No se detectaron diferencias en los archivos Gradle revisados."
  echo "No se iniciará la compilación."
  exit 3
fi

echo
echo "Revisa las diferencias de arriba. Para compilar con esta preparación temporal,"
echo "escribe exactamente BUILD. Cualquier otra respuesta cancela y restaura."
read -r -p "Respuesta: " ANSWER

if [ "$ANSWER" != "BUILD" ]; then
  echo "Cancelado."
  exit 0
fi

echo
echo "=== Compilando debug y ejecutando lint ==="
echo "No se instala la app ni se inicia la cámara."
studio build "$PROJECT" :app:assembleDebug :app:lintDebug

APK="$PROJECT/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -s "$APK" ]; then
  echo "Build terminó sin encontrar el APK esperado: $APK"
  exit 4
fi

echo
echo "=== APK generado ==="
ls -lh "$APK"

SIGNER="$SDK/build-tools/36.0.0/apksigner"
if [ -x "$SIGNER" ]; then
  "$SIGNER" verify --verbose "$APK"
elif command -v apksigner >/dev/null 2>&1; then
  apksigner verify --verbose "$APK"
else
  echo "Aviso: apksigner no disponible; firma no verificada."
fi

restore_compat
echo
echo "Build/lint completados; los cambios temporales de compatibilidad fueron restaurados."
echo "La app aún no se instaló y la cámara no se activó."
