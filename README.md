<p align="center">
  <img src="shared/icons/duck.png" width="96" alt="Audio Ducking icon">
</p>

<h1 align="center">Audio Ducking</h1>

<p align="center">
  Don't like constantly fiddling with the volume when someone starts talking? <br>Audio Ducking is here to help.<br><br>
  Available for: Android, Linux and Windows
</p>

<p align="center">
  <a href="README.pl.md">🇵🇱 Polski</a> ·
  <a href="#how-it-works">How it works</a> ·
  <a href="#building-from-source">Build</a>
</p>

---

<p align="center">
  <img src="docs/screenshots/android-idle.png" width="230" alt="Android app, not listening">
  &nbsp;
  <img src="docs/screenshots/android-ducking.png" width="230" alt="Android app, lowering the volume while someone talks">
  &nbsp;
  <img src="docs/screenshots/desktop.png" width="300" alt="Desktop app on Linux, podcast mode">
</p>

## About
The application was built first as an aid to stop fiddling with the radio volume when you're driving and you start having a conversation with someone, but quickly evolved into something bigger that can be used in pretty much any environment.
Audio Ducking listens through the microphone,
recognises **human speech** (and ignores music, typing, traffic…), and lowers or pauses
whatever is playing until the conversation is over.

The app was built as my **engineering thesis**.
All audio is processed on the device: **nothing is recorded, stored or sent anywhere.** - the Android app has **no internet permission**, so you do not have to worry about any tracking.

## Features

| | |
|---|---|
| 🦆 **Duck mode** | Lowers the device's volume while someone talks to a certain point. |
| 🔊 **Noise mode** | Raises the device's volume so you can keep listening over the talking. |
| 🎧 **Podcast mode** | Pauses playback while someone talks, then resumes a few seconds *back*, so you don't miss a word. |
| 🗣️ **Registered voices** | Optionally react only to people you've registered (speaker recognition), e.g. your partner but not strangers on a train. |
| 🚗 **Car mode** (Android) | Raises the volume as you drive faster or accelerate |
| ⚡ **Quick Settings tile** | Start and stop from the notification shade without opening the app. |
| 🫧 **Floating status** | A small overlay bubble that shows when the app is listening or ducking. |
| 🌍 **4 languages** | English, Polski, Deutsch, Español (more to come). There's also a built-in setup guide. |

<p align="center">
  <img src="docs/screenshots/android-tile.png" width="260" alt="Quick Settings tile">
</p>


## How it works

1. **Listening.** The microphone is read continuously at 16 kHz, in a foreground service on Android.
2. **Is it speech?** About twice a second, the last second of audio goes to
   [CED](https://github.com/RicherMans/CED), a sound classifier trained on Google's AudioSet
   (527 sound classes). If the *Speech* score is above the threshold, someone is talking.
3. **Speech gate.** Short pauses between sentences shouldn't make the music jump up and down.
   So ducking starts on speech, *holds* for a moment after it stops, and survives a short pause
   if someone answers within it.
4. **Whose voice?** *(optional)* The speech is turned into a voice "fingerprint" (embedding) by
   [ERes2Net](https://github.com/modelscope/3D-Speaker) and compared, by cosine similarity, with the
   voices you registered. Registering means reading a short text aloud. Silero VAD cuts out the
   silence, and extra fingerprints with background music and room echo can be added for robustness.
5. **Action.** The volume is changed through the system mixer, or the player is paused and rewound
   through media controls.

**Car mode** runs alongside: it maps GPS speed to volume steps, adds a boost when you accelerate,
and adds a boost when background noise rises. Every change is smoothed so it never jumps.

### Platform specifics

| | Android | Linux | Windows |
|---|---|---|---|
| Volume | `AudioManager` | PipeWire `wpctl` / PulseAudio `pactl` | Core Audio `IAudioEndpointVolume` (JNA) |
| Pause / play | Media key events | MPRIS over D-Bus | Media play/pause key |
| Rewind (podcast) | `MediaSession` (needs notification access) | MPRIS `Seek` | – |
| Runs in background | Foreground service + Quick Settings tile | Desktop window | Desktop window |

## Tech stack

- **Kotlin Multiplatform** with **Compose Multiplatform**: one UI and one detection pipeline for Android and desktop
- **[sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)** (ONNX Runtime) for on-device inference
- Models: **CED-base** (audio tagging, int8), **ERes2Net** (speaker verification), **Silero VAD**
- Android: foreground service, `TileService`, overlay window, GPS speed from `LocationManager`
- Desktop: JNA (Windows Core Audio), D-Bus / PipeWire command-line tools (Linux)

## Project structure

```
shared/                 Kotlin Multiplatform module
  src/commonMain/       UI (Compose), speech processor, speech gate, modes, car-mode logic, settings, translations
  src/desktopMain/      Desktop entry point, microphone, volume & media control for Linux/Windows
  src/commonTest/       Unit tests (speech gate, processor, car mode…)
app/                    Android app: service, Quick Settings tile, overlay, model loading
models/                 ONNX models and the music clips used for voice augmentation
packaging/              Scripts that build the APK, AppImage and portable Windows build
.github/workflows/      CI and release builds
```

## Building from source

Requirements: JDK 21, Android SDK (for the Android app). The models are already in `models/`.

```bash
# Run the desktop app
./gradlew :shared:run

# Tests
./gradlew :shared:desktopTest

# Android debug APK
./gradlew :app:assembleDebug

# Release-style packages (see the comments at the top of each script)
packaging/build-apk.sh
packaging/build-appimage.sh
packaging/build-windows.sh    # cross-builds from Linux using wine
```

Pushing a tag like `v1.0.0` makes GitHub Actions build all packages and attach them to a release.

## Permissions (Android)

| Permission | Why |
|---|---|
| Microphone | To hear when someone talks. Audio never leaves the device. |
| Notifications | For the "listening" notification that keeps the service running. |
| Location *(car mode only)* | GPS speed and acceleration. |
| Display over other apps *(optional)* | The floating status bubble. |
| Notification access *(optional)* | Rewinding in podcast mode. |

## Limitations

- iOS is not supported: iOS doesn't let apps change the system volume or control other apps' playback.
- macOS isn't supported yet. It would need native volume and media control.
- Speaker recognition gets less reliable with loud music in the background, and the *extra samples* option helps with that, although might not be perfect - having the microphone close to the area where the speech is happening will help with the recognition.

## License

[GPL-3.0](LICENSE) © 2026 nikoraasu.
Bundled models and music are under their own licenses. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
