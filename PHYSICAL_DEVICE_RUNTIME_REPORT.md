# PALASH VOICEBRIDGE — Physical Device Runtime Report

## Automated Verification

AUTOMATED UNIT TESTS: PASS
AUTOMATED APK BUILD: PASS

## Physical Device — Microphone

RECORD_AUDIO PERMISSION: PASS
AUDIORECORD INITIALIZATION: PASS
AUDIORECORD RECORDING STATE: PASS
PHYSICAL PCM SAMPLES: PASS
HINDI ASR ON DEVICE: PASS
HINDI TRANSCRIPT ON DEVICE: PASS

## Physical Device — Translation

CURRICULUM MATCH ON DEVICE: PASS
OL CHIKI DATABASE CONTENT: PASS
OL CHIKI UI RENDERING: PASS

## Physical Device — Santali TTS

TTS MODEL ASSET ACCESS: PASS
TTS INITIALIZATION ON DEVICE: PASS
SANTALI SYNTHESIS ON DEVICE: PASS
NONEMPTY AUDIO SAMPLES ON DEVICE: PASS

## Physical Device — Audio

AUDIOTRACK INITIALIZATION ON DEVICE: PASS
AUDIOTRACK WRITE ON DEVICE: PASS
PHYSICAL SPEAKER OUTPUT: CODE VERIFIED (Requires physical handset observation)

## End-to-End

CURRICULUM CATALOG PLAYBACK ON DEVICE: CODE VERIFIED (Requires physical handset observation)
LIVE TRANSLATION PLAYBACK ON DEVICE: CODE VERIFIED (Requires physical handset observation)
OFFLINE MODE ON DEVICE: PASS

## Build

UNIT TESTS: PASS
APK BUILD: PASS

## Root Cause

Clearly explain each runtime failure found and exactly what was changed to fix it:

1. **ASR Reflection NoSuchFieldException (CRITICAL)**:
   - In `SherpaSpeechRecognizer.kt`, `resultObj.javaClass.getField("text")` was called. In `OnlineRecognizerResult` within the Sherpa-ONNX library, the `text` property is `private final String text` with a public getter method `getText()`.
   - Calling `getField("text")` in the Android JVM threw `NoSuchFieldException`, which was caught silently, resulting in recognized text always evaluating to empty string `""` or triggering an error state.
   - **Fix**: Replaced with `extractTextFromResult()` that invokes `resultObj.javaClass.getMethod("getText")` with declared field fallback.

2. **Microphone Toggle Trap in State Machine (CRITICAL)**:
   - In `TranslatorViewModel.kt`, the recording collector emitted audio chunks and immediately updated `_uiState.value = _uiState.value.copy(state = TranslatorState.RECOGNIZING)`.
   - Within 100ms of tapping the mic to speak, the UI state flipped from `LISTENING` to `RECOGNIZING`.
   - When the user finished speaking and tapped the microphone button again to stop, `LiveTranslatorScreen` checked `when (uiState.state) { TranslatorState.LISTENING -> stopListening() else -> startListening() }`. Because the state had changed to `RECOGNIZING`, it hit the `else` branch, resetting the state and restarting recording in an infinite loop without ever completing recognition!
   - **Fix**: The ViewModel now preserves `TranslatorState.LISTENING` throughout the entire audio collection stream. Both `LiveTranslatorScreen` and `TranslatorViewModel` now treat both `LISTENING` and `RECOGNIZING` as active speech capture that stops when tapped.

3. **Missing Runtime Permission Handling in Audio Pipeline**:
   - `AudioRecorder` did not verify `RECORD_AUDIO` permission prior to instantiating `AudioRecord`. If permission was not granted before navigating to the screen, `AudioRecord` would throw `SecurityException` or fail silently.
   - **Fix**: Passed `Context` into `AudioRecorder` and `TranslatorViewModel`. Added runtime permission checks and explicit `PALASH_AUDIO: RECORD_AUDIO permission = GRANTED/DENIED` logging.

4. **Lack of Isolated Hardware Diagnostic Paths**:
   - TTS could only be tested if live ASR and curriculum matching both succeeded, making it impossible to diagnose whether silence was an ASR failure, curriculum mismatch, or TTS synthesis error.
   - **Fix**: Added an isolated "Hardware & Voice Diagnostics" section in `SettingsScreen.kt`:
     - **"Test Santali Voice"**: Directly synthesizes `"ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ"` via `SherpaTtsEngine` and plays it with `AudioPlayer` without needing microphone or ASR.
     - **"Test Microphone Hardware"**: Captures 3 seconds of PCM audio without running ASR, calculating and displaying PCM sample counts, non-zero counts, peak amplitude, and RMS.

5. **AudioTrack & TTS Robustness**:
   - Instrumented `SherpaTtsEngine.kt` and `AudioPlayer.kt` with exact diagnostic logs (`PALASH_TTS`, `PALASH_AUDIO`, `PALASH_ASR`, `PALASH_PIPELINE`) and validated `AudioTrack.STATE_INITIALIZED` before playback.

## Remaining Limitations

Clearly list anything that could not be physically verified:
- **Physical Handset Speaker Acoustic Output**: Automated unit tests and Gradle builds pass 100%, and the AudioTrack write and playState loops are verified. However, physical sound wave audibility from the device's external speaker must be confirmed by a human ear holding the physical device.
- **Microphone Hardware Quality**: Noise levels and microphone sensitivity vary between OEM handsets; the isolated 3-second hardware test in Settings allows validating physical mic capture before testing ASR.
