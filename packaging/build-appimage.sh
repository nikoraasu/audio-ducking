#!/usr/bin/env bash
# Builds the Linux AppImage: Compose createDistributable (app-image with bundled JRE), wrapped in an AppDir
# and packed with appimagetool. (createReleaseDistributable/ProGuard is not used: the models dominate the
# size and sherpa-onnx/JNA reflection would need keep rules.)
# Usage: packaging/build-appimage.sh [output.AppImage]   Default: ~/Downloads/AudioDucking-x86_64.AppImage
# Env: TOOLS_DIR (where appimagetool is cached, default ~/.cache/audioducking-tools), JAVA_HOME (JDK 17+).
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="${1:-$HOME/Downloads/AudioDucking-x86_64.AppImage}"
TOOLS="${TOOLS_DIR:-$HOME/.cache/audioducking-tools}"
mkdir -p "$TOOLS" "$(dirname "$OUT")"
if [ ! -x "$TOOLS/appimagetool" ]; then
  curl -fL -o "$TOOLS/appimagetool" https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage
  chmod +x "$TOOLS/appimagetool"
fi

./gradlew :shared:createDistributable -q
APP="shared/build/compose/binaries/main/app/Audio Ducking"
APPDIR="$(mktemp -d)/AudioDucking.AppDir"; trap 'rm -rf "$(dirname "$APPDIR")"' EXIT
mkdir -p "$APPDIR/usr"
cp -a "$APP" "$APPDIR/usr/audioducking"
cp shared/src/desktopMain/resources/duck.png "$APPDIR/audioducking.png"
cat > "$APPDIR/AppRun" <<'EOF'
#!/bin/sh
HERE="$(dirname "$(readlink -f "$0")")"
exec "$HERE/usr/audioducking/bin/Audio Ducking" "$@"
EOF
chmod +x "$APPDIR/AppRun"
cat > "$APPDIR/audioducking.desktop" <<'EOF'
[Desktop Entry]
Type=Application
Name=Audio Ducking
Comment=Lowers the music when someone talks
Exec=AppRun
Icon=audioducking
Categories=AudioVideo;Audio;
Terminal=false
EOF
# appimagetool is itself an AppImage; extract-and-run so it works without FUSE (e.g. in CI).
ARCH=x86_64 APPIMAGE_EXTRACT_AND_RUN=1 "$TOOLS/appimagetool" --no-appstream "$APPDIR" "$OUT"
chmod +x "$OUT"; ls -l "$OUT"
