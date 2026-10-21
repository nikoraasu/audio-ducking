# Third-party notices

Audio Ducking is licensed under the GPL-3.0 (see `LICENSE`). It bundles and uses the following third-party
material under their own licenses.

## Bundled models (`models/`)

| File | What | License | Source |
|---|---|---|---|
| `silero_vad.onnx` | Silero VAD, voice activity detection | MIT (Copyright Silero Team) | https://github.com/snakers4/silero-vad |
| `ced_base_int8.onnx` | CED base audio tagger (AudioSet), int8 quantised | The CED repository is GPL-3.0 (the weights carry no separate license statement there; compatible with this app being GPL-3.0). sherpa-onnx itself is Apache-2.0 | Original: https://github.com/RicherMans/CED. Packaged for sherpa-onnx: https://github.com/k2-fsa/sherpa-onnx/releases/tag/audio-tagging-models (`sherpa-onnx-ced-base-audio-tagging-2024-04-19`). Which of these two the int8 file was taken from is not recorded in this repo (TODO verify) |
| `ced_labels.csv` | The 527 AudioSet class names used with CED | Google publishes the AudioSet ontology under CC BY-SA 4.0 (the dataset under CC BY 4.0) | https://research.google.com/audioset/download.html |
| `eres2net_sv_en_voxceleb_16k.onnx` | 3D-Speaker ERes2Net speaker embedding, trained on VoxCeleb | Apache-2.0 (3D-Speaker repository) | https://github.com/modelscope/3D-Speaker; converted for sherpa-onnx as `3dspeaker_speech_eres2net_sv_en_voxceleb_16k.onnx`: https://github.com/k2-fsa/sherpa-onnx/releases/tag/speaker-recongition-models |

## Bundled music (`models/augment_music_1.pcm`, `augment_music_2.pcm`)

20-second excerpts, converted to raw 16-bit mono PCM at 16 kHz, used only to make the "voice with music"
copies at registration:

- "Hackbeat" by Kevin MacLeod (incompetech.com)
- "Volatile Reaction" by Kevin MacLeod (incompetech.com)

Licensed under Creative Commons: By Attribution 4.0, https://creativecommons.org/licenses/by/4.0/
(the same credit appears in the app under Voices, "extra samples").

## Libraries

- sherpa-onnx (k2-fsa), https://github.com/k2-fsa/sherpa-onnx, Apache-2.0. Android AAR/JNI and desktop JVM jars and native libraries. Includes ONNX Runtime (MIT).
- Compose Multiplatform and Jetpack Compose / AndroidX (JetBrains, Google), Apache-2.0.
- Kotlin and kotlinx.coroutines (JetBrains), Apache-2.0.
- JNA (`jna-platform`), https://github.com/java-native-access/jna, Apache-2.0 OR LGPL-2.1-or-later (your choice); used under Apache-2.0.
