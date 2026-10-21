#!/usr/bin/env bash
# Builds a portable Windows x64 zip from Linux: Gradle collects the Windows runtime classpath
# (:shared:collectWindowsApp), then the Windows jpackage from a Temurin 21 JDK runs under wine to create an
# app-image ("Audio Ducking.exe" + bundled runtime), which is zipped. No installer, no Java needed to run.
# Usage: packaging/build-windows.sh [output.zip]   Default: ~/Downloads/AudioDucking-Windows-x64.zip
# Needs: wine, curl, unzip, zip. Env: TOOLS_DIR (cache for the JDK and the wine prefix,
# default ~/.cache/audioducking-tools), JAVA_HOME (JDK 17+ for Gradle).
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$PWD"
OUT="${1:-$HOME/Downloads/AudioDucking-Windows-x64.zip}"
TOOLS="${TOOLS_DIR:-$HOME/.cache/audioducking-tools}/win"
mkdir -p "$TOOLS" "$(dirname "$OUT")"
export WINEDEBUG=-all WINEPREFIX="$TOOLS/prefix"

if [ ! -d "$TOOLS/jdk" ]; then
  curl -fL -o "$TOOLS/jdk.zip" "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse"
  mkdir "$TOOLS/jdk-unpack" && unzip -q "$TOOLS/jdk.zip" -d "$TOOLS/jdk-unpack"
  mv "$TOOLS/jdk-unpack"/* "$TOOLS/jdk" && rmdir "$TOOLS/jdk-unpack"
fi

./gradlew :shared:collectWindowsApp -q
# jpackage refuses a destination outside the wine prefix's C: drive ("not writable"), so build there.
wineboot -u >/dev/null 2>&1 || true
DEST="$WINEPREFIX/drive_c/audioducking-out"
rm -rf "$DEST"; mkdir -p "$DEST"
wine "$TOOLS/jdk/bin/jpackage.exe" --type app-image --name "Audio Ducking" \
  --input "$ROOT/shared/build/windows/app" --main-jar shared-desktop.jar \
  --main-class io.github.nikoraasu.audioducking.MainKt \
  --icon "$ROOT/shared/icons/duck.ico" --dest 'C:\audioducking-out' \
  --add-modules java.base,java.desktop,java.logging,java.prefs,java.management,java.naming,java.sql,java.instrument,jdk.unsupported \
  2>&1 | grep -v "MESA\|pci id\|affinity" || true
[ -f "$DEST/Audio Ducking/Audio Ducking.exe" ] || { echo "jpackage failed" >&2; exit 1; }

rm -f "$OUT"
(cd "$DEST" && zip -qr "$OUT" "Audio Ducking")
ls -l "$OUT"
