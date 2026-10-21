#!/usr/bin/env bash
# Builds the Android APK: assembleDebug, strips x86_64 libs, 16 KB zipalign, signs with the debug keystore
# (fine for sideloading; replace with a release keystore for store distribution).
# Usage: packaging/build-apk.sh [output.apk]    Output default: ~/Downloads/AudioDucking-v1.0.apk
# Needs: Android SDK build-tools (ANDROID_HOME or ~/Android/Sdk), ~/.android/debug.keystore.
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="${1:-$HOME/Downloads/AudioDucking-v1.0.apk}"
SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
BT="$SDK/build-tools/$(ls "$SDK/build-tools" | sort -V | tail -1)"
KS="${KEYSTORE:-$HOME/.android/debug.keystore}"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT

./gradlew :app:assembleDebug -q
cp app/build/outputs/apk/debug/app-debug.apk "$TMP/in.apk"
zip -q -d "$TMP/in.apk" 'lib/x86_64/*' || true
"$BT/zipalign" -P 16 -f 4 "$TMP/in.apk" "$TMP/aligned.apk"
mkdir -p "$(dirname "$OUT")"
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android --ks-key-alias androiddebugkey --out "$OUT" "$TMP/aligned.apk"
"$BT/apksigner" verify "$OUT"
n=$("$BT/aapt2" dump xmltree --file AndroidManifest.xml "$OUT" | grep -ic testOnly || true)
[ "$n" = 0 ] || { echo "APK is testOnly" >&2; exit 1; }
"$BT/aapt2" dump badging "$OUT" | grep -E "^package|native-code"
ls -l "$OUT"
