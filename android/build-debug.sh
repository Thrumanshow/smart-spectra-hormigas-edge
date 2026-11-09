#!/data/data/com.termux/files/usr/bin/bash
set -eu

ROOT="$(cd "$(dirname "$0")" && pwd)"

if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle no está disponible. No se instaló nada."
  exit 2
fi

if [ -z "${ANDROID_HOME:-}" ] && [ -z "${ANDROID_SDK_ROOT:-}" ]; then
  echo "ANDROID_HOME/ANDROID_SDK_ROOT no está configurado."
  echo "No se inició la compilación."
  exit 2
fi

exec gradle --no-daemon -p "$ROOT" :app:assembleDebug
