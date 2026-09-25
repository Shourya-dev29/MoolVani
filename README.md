# PALASH VoiceBridge (moolvani)

> **"Teach in Hindi. Learn in Your Mother Tongue."**

**AI-Powered Vernacular Pedagogy & Offline Real-Time Translation Platform for Mother Tongue-Based Primary Education**

**Smart India Hackathon (SIH 2026)**  
**Problem Statement ID:** 26042  
**Organization:** Government of Jharkhand  
**Department:** Higher & Technical Education  
**Domain:** Smart Education / Low-Resource Edge NLP  
**Platform:** Android  
**Minimum Android Version:** Android 9 (API 28+)  
**Operation:** 100% Offline

---

# 1. Overview

PALASH VoiceBridge (`moolvani`) is an **offline AI-powered Android application** designed to help Hindi-medium teachers communicate and teach foundational literacy and numeracy content to children whose mother tongue is **Santhali**.

The application combines:

- Offline Hindi Speech Recognition
- Deterministic curriculum-based translation
- Native Santali speech synthesis
- Ol Chiki script support
- Offline classroom worksheets
- Visual flashcard generation
- NIPUN Bharat / FLN curriculum content
- Android tablet optimized UI
- No internet dependency

The system is designed specifically for environments where:

- Internet connectivity is unreliable or unavailable.
- Cloud APIs cannot be depended upon.
- Android tablets may have as little as 2 GB RAM.
- Teachers need simple voice-based interaction.
- Translation must remain predictable and safe for classroom use.

---

# 2. Problem Statement

## SIH PS 26042

Jharkhand's Mother Tongue-Based Multilingual Education initiative aims to improve foundational education for children from indigenous communities.

A major classroom challenge is the language gap between:

**Hindi-medium teachers**

and

**tribal-language-speaking students.**

Languages such as Santhali, Ho and Mundari have significantly fewer digital NLP resources than major Indian languages.

Existing cloud translation systems are unsuitable for remote classrooms because they can require:

- Internet connectivity
- Continuous API access
- High bandwidth
- Cloud processing
- External services

PALASH VoiceBridge addresses this problem by moving the core translation pipeline directly onto the Android device.

---

# 3. Solution

The application follows an offline voice-to-voice classroom pipeline:

```text
Hindi Teacher
     │
     ▼
Microphone
     │
     ▼
AudioRecord
16 kHz / 16-bit / Mono PCM
     │
     ▼
Offline Hindi ASR
Sherpa-ONNX
     │
     ▼
Hindi Text
     │
     ▼
Hindi Normalization
     │
     ▼
Curriculum Matcher
     │
     ├── Exact Match
     │
     └── Levenshtein Fuzzy Match
     │
     ▼
Verified Curriculum Translation
     │
     ▼
Santali / Ol Chiki Text
     │
     ▼
Offline Santali TTS
Sherpa-ONNX VITS / Piper
     │
     ▼
PCM Audio
     │
     ▼
AudioTrack
     │
     ▼
Students


# PALASH VoiceBridge (moolvani)

> **"Teach in Hindi. Learn in Your Mother Tongue."**

**AI-Powered Vernacular Pedagogy & Offline Real-Time Translation Platform for Mother Tongue-Based Primary Education**

**Smart India Hackathon (SIH 2026)**  
**Problem Statement ID:** 26042  
**Organization:** Government of Jharkhand  
**Department:** Higher & Technical Education  
**Domain:** Smart Education / Low-Resource Edge NLP  
**Platform:** Android  
**Minimum Android Version:** Android 9 (API 28+)  
**Operation:** 100% Offline

---

# 1. Overview

PALASH VoiceBridge (`moolvani`) is an **offline AI-powered Android application** designed to help Hindi-medium teachers communicate and teach foundational literacy and numeracy content to children whose mother tongue is **Santhali**.

The application combines:

- Offline Hindi Speech Recognition
- Deterministic curriculum-based translation
- Native Santali speech synthesis
- Ol Chiki script support
- Offline classroom worksheets
- Visual flashcard generation
- NIPUN Bharat / FLN curriculum content
- Android tablet optimized UI
- No internet dependency

The system is designed specifically for environments where:

- Internet connectivity is unreliable or unavailable.
- Cloud APIs cannot be depended upon.
- Android tablets may have as little as 2 GB RAM.
- Teachers need simple voice-based interaction.
- Translation must remain predictable and safe for classroom use.

---

# 2. Problem Statement

## SIH PS 26042

Jharkhand's Mother Tongue-Based Multilingual Education initiative aims to improve foundational education for children from indigenous communities.

A major classroom challenge is the language gap between:

**Hindi-medium teachers**

and

**tribal-language-speaking students.**

Languages such as Santhali, Ho and Mundari have significantly fewer digital NLP resources than major Indian languages.

Existing cloud translation systems are unsuitable for remote classrooms because they can require:

- Internet connectivity
- Continuous API access
- High bandwidth
- Cloud processing
- External services

PALASH VoiceBridge addresses this problem by moving the core translation pipeline directly onto the Android device.

---

# 3. Solution

The application follows an offline voice-to-voice classroom pipeline:

```text
Hindi Teacher
     │
     ▼
Microphone
     │
     ▼
AudioRecord
16 kHz / 16-bit / Mono PCM
     │
     ▼
Offline Hindi ASR
Sherpa-ONNX
     │
     ▼
Hindi Text
     │
     ▼
Hindi Normalization
     │
     ▼
Curriculum Matcher
     │
     ├── Exact Match
     │
     └── Levenshtein Fuzzy Match
     │
     ▼
Verified Curriculum Translation
     │
     ▼
Santali / Ol Chiki Text
     │
     ▼
Offline Santali TTS
Sherpa-ONNX VITS / Piper
     │
     ▼
PCM Audio
     │
     ▼
AudioTrack
     │
     ▼
Students


moolvani/
│
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/palash/voicebridge/
│   │       │
│   │       │   ├── data/
│   │       │   ├── domain/
│   │       │   │   └── tts/
│   │       │   │       └── SherpaTtsEngine.kt
│   │       │   │
│   │       │   ├── models/
│   │       │   │   └── ModelManager.kt
│   │       │   │
│   │       │   ├── ui/
│   │       │   │   ├── home/
│   │       │   │   ├── translator/
│   │       │   │   ├── curriculum/
│   │       │   │   ├── worksheets/
│   │       │   │   ├── flashcards/
│   │       │   │   └── settings/
│   │       │   │
│   │       │   └── MainActivity.kt
│   │       │
│   │       ├── assets/
│   │       │   └── models/
│   │       │       ├── asr/
│   │       │       │   └── hindi/
│   │       │       │
│   │       │       └── tts/
│   │       │           ├── santali/
│   │       │           └── santhali/
│   │       │
│   │       └── AndroidManifest.xml
│   │
│   ├── libs/
│   │   └── sherpa-onnx.aar
│   │
│   └── build.gradle.kts
│
├── README.md
├── UI_CHANGE_REPORT.md
├── TTS_INITIALIZATION_FIX_REPORT.md
└── 042 ppt



Build Requirements

Recommended environment:

Android Studio Ladybug or newer
JDK 21
Android SDK
Android SDK Platform 34
Android NDK
CMake

Target configuration:

compileSdk = 34
minSdk = 28

Supported devices:

Android 9+

User Interface

The UI was redesigned specifically for classroom use.

The interface follows:

Neutral colors
High text visibility
Large touch targets
Clear labels
Simple navigation
Responsive tablet layouts
Clear AI state indicators
Accessible controls
Minimal technical jargon

The primary voice interaction is intentionally prominent.

Worksheet Generator

Worksheets are generated entirely on-device.

Technology:

android.graphics.pdf.PdfDocument

A4 dimensions:

595 × 842

The generator creates:

Hindi vocabulary
Santali translation
Ol Chiki text
Tracing areas
Classroom activities
Printable layouts

No online PDF generation service is required.

Flashcard Generator

Flashcards are also generated completely offline.

The system creates printable A4 sheets containing multiple cards.

Each card can contain:

Visual
Hindi Word
Santali / Ol Chiki Word

The output is a standard PDF that can be opened or printed from Android.

Offline Architecture

The application deliberately does not request the Android Internet permission.

Verified:

android.permission.INTERNET

is absent from:

app/src/main/AndroidManifest.xml

Therefore the application architecture does not depend on:

Cloud APIs
Google Translate
OpenAI APIs
Remote databases
Online authentication
External translation services

The core pipeline executes locally.


┌──────────────────────────────────────────────┐
│              PALASH VOICEBRIDGE              │
│                  Android App                 │
└──────────────────────┬───────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────┐
│                  UI Layer                    │
│                                              │
│ Home | Translator | Curriculum | Worksheets │
│ Flashcards | Settings                        │
└──────────────────────┬───────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────┐
│               Application Layer              │
│                                              │
│ ViewModels | Pipeline Controller | State     │
└──────────────────────┬───────────────────────┘
                       │
             ┌─────────┴─────────┐
             │                   │
             ▼                   ▼
┌─────────────────────┐   ┌─────────────────────┐
│   Offline ASR       │   │   Offline TTS       │
│                     │   │                     │
│ Sherpa-ONNX         │   │ Sherpa-ONNX         │
│ Hindi Zipformer CTC │   │ VITS / Piper        │
└──────────┬──────────┘   └──────────┬──────────┘
           │                         │
           ▼                         │
┌─────────────────────┐              │
│ Hindi Normalizer    │              │
└──────────┬──────────┘              │
           │                         │
           ▼                         │
┌─────────────────────┐              │
│ Curriculum Matcher  │              │
│ Exact + Fuzzy       │              │
└──────────┬──────────┘              │
           │                         │
           ▼                         │
┌─────────────────────┐              │
│ Room / SQLite DB    │              │
│ 135+ Curriculum     │              │
└──────────┬──────────┘              │
           │                         │
           ▼                         ▼
┌─────────────────────┐   ┌─────────────────────┐
│ Santali / Ol Chiki  │──►│ Offline TTS         │
│ Translation         │   │                     │
└─────────────────────┘   └──────────┬──────────┘
                                     │
                                     ▼
                            ┌───────────────────┐
                            │    AudioTrack     │
                            └─────────┬─────────┘
                                      │
                                      ▼
                                   Speaker


Mission

PALASH VoiceBridge is built around a simple idea:

A teacher should not need internet connectivity or years of language training to communicate effectively with children in their mother tongue.

By combining offline speech recognition, deterministic curriculum translation, native-language speech synthesis and locally generated learning materials, PALASH VoiceBridge aims to make mother-tongue-based primary education more accessible in low-resource environments.


License

This project is developed as a Smart India Hackathon solution for:

SIH 2026
Problem Statement 26042
Government of Jharkhand
Department of Higher & Technical Education

Application source code:

Apache-2.0

Third-party models and libraries retain their respective licenses.

PALASH VoiceBridge
Offline AI for Mother-Tongue Education

Hindi → Santali → Ol Chiki → Voice

No Cloud. No Internet. No Translation API.

Built for classrooms where connectivity cannot be assumed.


