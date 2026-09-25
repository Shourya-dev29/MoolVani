# TTS & Ol Chiki Verification Report

## Problem Found
1. **TTS Asset Path & Code Mismatch**:
   - `ModelManager` was initializing `SherpaTtsEngine` with language code `"sat"`, but the directory in `app/src/main/assets/models/tts/` was named `santali`. `SherpaTtsEngine.isModelAvailable` and `initialize` looked strictly at `models/tts/$languageCode` (`models/tts/sat`), which caused model detection/initialization to fail when searching solely for "sat".
2. **GeneratedAudio Reflection Incompatibility**:
   - Sherpa-ONNX's Kotlin `GeneratedAudio` class declares `private final float[] samples` and `private final int sampleRate`, with public getters `getSamples()` and `getSampleRate()`. `SherpaTtsEngine` attempted `audioObj.javaClass.getField("samples")`, which throws `NoSuchFieldException` on private fields when accessible getters are present.
3. **Play Translation State Disconnect**:
   - In `TranslatorViewModel`, `playAgain()` depended solely on `lastAudioData`. If audio was cleared or not retained from a catalog selection, pressing "Play Translation" did nothing instead of re-synthesizing the active Ol Chiki translation.
   - `TranslatorViewModel` did not preserve `sampleRate` between synthesis and re-playback, defaulting to 16kHz even if the TTS model produced a different rate.
4. **Status Truthfulness (Readiness vs. Installed)**:
   - `ModelManager` reported `READY` even when only assets were present without checking runtime availability.
   - `OfflineStatusManager` lacked an explicit `INSTALLED` ("Installed — not ready") distinction.

## Root Cause
- Reflection calls in `SherpaTtsEngine` targeted private fields rather than Kotlin JavaBean getters (`getSamples()`, `getSampleRate()`).
- Folder resolution did not map standard BCP-47 ISO language code `"sat"` to the asset directory name `santali`.
- Re-synthesis fallback was missing in `playAgain()` when `lastAudioData` was null but `translatedText` was present.

## Files Changed
- `SherpaTtsEngine.kt`:
  - Added multi-candidate directory resolution (`santali`, `santhali`, `sat`) for `isModelAvailable` and `initialize`.
  - Added primary getter invocation (`getSamples()`, `getSampleRate()`) with field fallback.
  - Added detailed diagnostic logs (`PALASH_TTS: initializing...`, `model loaded`, `synthesis started`, `samples generated = X`, `PALASH_TTS_ERROR: ...`).
- `AudioPlayer.kt`:
  - Added `track.write` return code verification.
  - Added playback loop guard on `track.playState == AudioTrack.PLAYSTATE_PLAYING`.
  - Added `PALASH_TTS: AudioTrack started` and `playback completed` diagnostic logging.
- `TranslatorViewModel.kt`:
  - Stored `lastSampleRate` alongside `lastAudioData`.
  - Added automatic re-synthesis fallback in `playAgain()` if `lastAudioData` is null but `translatedText` is populated.
  - Added teacher-friendly error reporting when TTS synthesis fails.
- `ModelManager.kt`:
  - Added dual verification of asset file existence and runtime class availability before setting `ModelStatus.READY`.
  - Added candidate path resolution for `santhali_tts`.
- `OfflineStatusManager.kt`:
  - Added `ComponentStatus.INSTALLED` ("Installed — not ready").
- `CurriculumExplorerScreen.kt`:
  - Added direct offline Santali speech synthesis and playback in `PhraseDetailSheet`.
- `EndToEndPipelineIntegrationTest.kt`:
  - Added automated token vocabulary coverage test verifying that every verified Ol Chiki curriculum phrase maps directly to `tokens.txt` of `sat_piper_model`.
  - Added `TtsResult` lifecycle and contract test.

## Santali Model
- Model Format: VITS / Piper ONNX (`sat_piper_model.onnx`, `sat_piper_model.onnx.json`, `tokens.txt`).
- Quality/Sample Rate: 16,000 Hz, 16-bit Mono PCM.
- Input Representation: Direct Ol Chiki script text (`phoneme_type: "text"` in JSON with 67 mapped Ol Chiki characters, punctuation, and numerals).

## Ol Chiki Rendering
- All 120+ curriculum seed entries use authentic Unicode Ol Chiki (U+1C50 - U+1C7F).
- Typography uses `MaterialTheme.typography.displaySmall` (28sp bold, 36sp line height) ensuring clear classroom visibility without vertical clipping or horizontal truncation.
- Font fallback leverages Android's system font fallback supporting Unicode Ol Chiki.

## TTS Runtime
- Dynamic binding with `com.k2fsa.sherpa.onnx.OfflineTts` via `sherpa-onnx.aar` in `app/libs/`.
- Dynamic configuration of `OfflineTtsModelConfig`, `OfflineTtsVitsModelConfig`, and `OfflineTtsConfig`.
- Reflection inspects `getSamples()` and `getSampleRate()` on `com.k2fsa.sherpa.onnx.GeneratedAudio`.

## Audio Playback
- Managed via `AudioPlayer` wrapping Android `AudioTrack` in `MODE_STATIC` with speech audio attributes.
- Buffer sized to `maxOf(minBufferSize, audioData.size * 2)`.
- Verified resource release and error trapping to prevent channel leaks.

## Offline Verification
- `AndroidManifest.xml` contains strictly zero internet permissions:
  `<uses-permission android:name="android.permission.RECORD_AUDIO" />`
- All models, tokens, dictionaries, and Room databases reside entirely inside on-device assets and local SQLite storage.

## Tests
- `testSantaliTtsModel_olChikiTokensCoverage`: **PASS** (100% of Ol Chiki characters in verified curriculum phrases exist in `tokens.txt`).
- `testTtsResult_successAndPlaybackContract`: **PASS**.
- `testEndToEnd_verifiedClassroomPhrasePipeline`: **PASS**.
- `testEndToEnd_fuzzyMatchingMinorVariation`: **PASS**.
- `testEndToEnd_safeRejectionOfUnrelatedPhrase`: **PASS**.
- `testLatencyTracker_multiTrialBenchmarking`: **PASS**.
- `testSherpaAdapters_gracefulHandlingWhenMissing`: **PASS**.
- `testCurriculumSeed_hasBothVerifiedAndReviewEntries`: **PASS**.

## Build
- Clean, unit tests, and APK compilation:
  - `./gradlew.bat clean`: **PASS**
  - `./gradlew.bat testDebugUnitTest`: **PASS** (100% tests pass)
  - `./gradlew.bat assembleDebug`: **PASS** (`app-debug.apk` built successfully)

## Remaining Limitations
- Audio audible output verified through `AudioTrack.write` and buffer progression logic; physical acoustic output requires running the APK on an Android device with physical speakers.
