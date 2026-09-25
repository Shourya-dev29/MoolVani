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
