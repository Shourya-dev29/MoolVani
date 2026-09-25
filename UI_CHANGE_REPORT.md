# PALASH VOICEBRIDGE (`moolvani`) — UI / UX Reference & Maintenance Report

This document serves as the permanent developer and designer guide for the user interface of **PALASH VoiceBridge** (SIH PS 26042). Every UI component, color token, typography style, voice state, and navigation destination is mapped directly to its concrete Kotlin source file in the project.

---

## 1. Quick Component Locator Table

| What I want to change | File / Location to Inspect | Exact Function / Composable |
| :--- | :--- | :--- |
| **Main screen layout** | `app/src/main/java/com/palash/voicebridge/ui/home/HomeScreen.kt` | `HomeScreen()` |
| **Voice button (Mic)** | `app/src/main/java/com/palash/voicebridge/ui/translator/LiveTranslatorScreen.kt` | `ProminentVoiceButtonSection()` |
| **Voice button text & labels** | `app/src/main/java/com/palash/voicebridge/ui/translator/LiveTranslatorScreen.kt` | `buttonText, buttonSubtext in ProminentVoiceButtonSection()` |
| **Hindi transcript ("What you said")** | `app/src/main/java/com/palash/voicebridge/ui/translator/LiveTranslatorScreen.kt` | `HindiTranscriptCard()` |
| **Ol Chiki mother-tongue translation** | `app/src/main/java/com/palash/voicebridge/ui/translator/LiveTranslatorScreen.kt` | `MotherTongueTranslationCard()` |
| **Play Translation & Translate Again** | `app/src/main/java/com/palash/voicebridge/ui/translator/LiveTranslatorScreen.kt` | `SecondaryActionControls()` |
| **Colours / Neutral palette** | `app/src/main/java/com/palash/voicebridge/ui/theme/Theme.kt` | `LightColorScheme, DarkColorScheme, val PalashGreen...` |
| **Typography / Font sizes** | `app/src/main/java/com/palash/voicebridge/ui/theme/Type.kt` | `PalashTypography (displaySmall, titleMedium, etc.)` |
| **App Navigation & Routes** | `app/src/main/java/com/palash/voicebridge/ui/navigation/NavGraph.kt` | `Screen sealed class & PalashNavGraph()` |
| **Worksheets UI** | `app/src/main/java/com/palash/voicebridge/ui/worksheets/WorksheetGeneratorScreen.kt` | `WorksheetGeneratorScreen() & WorksheetControlsCard()` |
| **Flashcards UI** | `app/src/main/java/com/palash/voicebridge/ui/flashcards/FlashcardGeneratorScreen.kt` | `FlashcardGeneratorScreen() & FlashcardControlsCard()` |
| **Settings & Diagnostics UI** | `app/src/main/java/com/palash/voicebridge/ui/settings/SettingsScreen.kt` | `SettingsScreen() & SettingsGroup()` |
| **Voice pipeline loading states** | `app/src/main/java/com/palash/voicebridge/ui/translator/LiveTranslatorScreen.kt` | `PipelineStageIndicator()` |
| **User-friendly error messages** | `app/src/main/java/com/palash/voicebridge/ui/translator/TranslatorViewModel.kt` | `startListening(), stopListening(), processRecognizedText()` |
| **App Top Bar & Back Navigation** | `app/src/main/java/com/palash/voicebridge/ui/components/CommonComponents.kt` | `PalashTopBar()` |
| **Offline status indicators** | `app/src/main/java/com/palash/voicebridge/ui/components/CommonComponents.kt` | `OfflineStatusBreakdownCard() & OfflineReadyChip()` |

---

## 2. Colour System

All application colors are centralized in `app/src/main/java/com/palash/voicebridge/ui/theme/Theme.kt`.

### Core Neutral Educational Palette
- **Primary Accent (`PalashGreen`)**: `#1B5E20` (Deep scholar forest green). Used for primary buttons, active state indicators, and headers.
- **Primary Container (`PalashGreenContainer`)**: `#E8F5E9` (Soft calm green). Used for translation result surfaces.
- **Background (`BackgroundLight`)**: `#F8F9FA` (Soft neutral grey-white). Clean, calm backdrop that reduces eye strain in classroom environments.
- **Surface (`SurfaceLight`)**: `#FFFFFF` (Pure white). Used for clean, elevated cards.
- **Surface Variant (`SurfaceVariantLight`)**: `#F1F3F5` (Subtle off-white). Used for secondary section cards.
- **Card Outline Border (`OutlineBorderLight`)**: `#E2E8F0` (Crisp light grey). Replaces heavy shadows with subtle contrast borders.
- **Primary Text (`TextPrimaryLight`)**: `#1A1C1A` (Deep charcoal, contrast ratio > 7:1 against surfaces).
- **Secondary Text (`TextSecondaryLight`)**: `#4A5568` (Muted slate, contrast ratio > 4.5:1).
- **Subtle Muted Text (`TextMutedLight`)**: `#64748B` (Metadata and helper labels).

### Status Indicators (Subtle & Restrained)
- **Offline Ready (`OfflineReadyGreen`)**: `#2E7D32`
- **Notice / Demo (`DemoModeAmber`)**: `#B45309` (Warm muted amber, never neon)
- **Error (`ErrorRed`)**: `#B91C1C`
- **Curriculum Verified (`VerifiedBlue`)**: `#1565C0`
- **Secondary Accent (`PalashEarth`)**: `#5D4037` (Warm earthy brown for Flashcards)

---

## 3. Typography Hierarchy

All typography styles are defined in `app/src/main/java/com/palash/voicebridge/ui/theme/Type.kt`.

- **Ol Chiki Translation Script (`displaySmall`)**: `fontSize = 28.sp`, `lineHeight = 36.sp`, `fontWeight = FontWeight.Bold`. Engineered specifically to give native Ol Chiki glyphs (`ᱫᱩᱲᱩᱵ ᱢᱮ`, `ᱯᱚᱛᱚᱵ`, etc.) high visibility across the classroom.
- **Teacher Hindi Speech (`headlineMedium`)**: `fontSize = 18.sp`, `lineHeight = 24.sp`, `fontWeight = FontWeight.Bold`. Clear, legible Devanagari script.
- **Card Titles (`titleMedium`)**: `fontSize = 16.sp`, `lineHeight = 24.sp`, `fontWeight = FontWeight.Bold`.
- **Button Labels (`titleMedium` / `labelLarge`)**: `fontSize = 15.sp–16.sp`, `fontWeight = FontWeight.Bold`.
- **Body Text (`bodyMedium` / `bodySmall`)**: `fontSize = 13.sp–15.sp`, `color = TextSecondaryLight`.

---

## 4. Button & Control Hierarchy

Every interactive control adheres to the **Icon + Readable Text** principle with a minimum touch target height of **48dp**.

1. **Primary Control**: 🎙️ **Tap to Speak** (`ProminentVoiceButtonSection`)
   - 112dp circular action button with animated states.
   - High visual priority: always positioned prominently at the top of the interaction flow.
2. **Secondary Controls**:
   - 🔊 **Play Translation** (`PalashGreen` container button, enabled when audio is available)
   - 🔄 **Translate Again** (Outlined tonal button, clears current session for a new phrase)
3. **Supporting Navigation Controls**:
   - 📚 **Curriculum**: Browse 120+ classroom phrases
   - 📝 **Worksheets**: Printable bilingual A4 practice sheets
   - 🃏 **Flashcards**: Visual flashcards with Hindi & Santali labels
   - ⚙️ **Settings**: Diagnostics and offline AI model manager
   - 📋 **Select from Curriculum Catalog**: Opens bottom sheet with 120+ pre-seeded NIPUN phrases

---

## 5. Screen Navigation Architecture

Navigation routes and screen destinations are managed in `app/src/main/java/com/palash/voicebridge/ui/navigation/NavGraph.kt`.

```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object LiveTranslator : Screen("live_translator")
    object CurriculumExplorer : Screen("curriculum_explorer")
    object WorksheetGenerator : Screen("worksheet_generator")
    object FlashcardGenerator : Screen("flashcard_generator")
    object Settings : Screen("settings")
}
```

Every secondary screen contains a standardized `PalashTopBar` with:
- Clear, readable screen title
- Contextual subtitle explaining purpose
- Standard back navigation arrow (`Icons.AutoMirrored.Filled.ArrowBack`) with touch target >= 48dp

---

## 6. Voice Pipeline State Machine

The voice translation state machine is defined in `app/src/main/java/com/palash/voicebridge/ui/translator/TranslatorViewModel.kt` (`enum class TranslatorState`):

```text
[IDLE / READY]  --> Tap to Speak
      ↓
[LISTENING]     --> Recording via AudioRecord, subtle pulse animation, tap to stop
      ↓
[RECOGNIZING]   --> Sherpa-ONNX streaming ASR inference ("Understanding...")
      ↓
[MATCHING]      --> Hindi normalization & Levenshtein curriculum lookup ("Finding curriculum translation...")
      ↓
[TRANSLATING]   --> Ol Chiki translation retrieved
      ↓
[SPEAKING]      --> Sherpa-ONNX Santali TTS synthesis & AudioTrack playback ("Playing translation...")
      ↓
[COMPLETED]     --> Result ready: "Play Translation" & "Translate Again" controls active
```

Error and safeguard states:
- `TRANSLATION_UNAVAILABLE`: Triggered when input does not match verified curriculum, protecting primary students from unverified translations.
- `ERROR`: Displays non-technical, human-friendly guidance (e.g., "Microphone unavailable. Please check microphone permission.").

---

## 7. How to Make Future UI Changes (Examples)

### Example 1: Changing the Microphone Button Appearance
1. Open `app/src/main/java/com/palash/voicebridge/ui/translator/LiveTranslatorScreen.kt`.
2. Locate the function `ProminentVoiceButtonSection(state, isDemoMode, onMicClick)`.
3. Modify the `Box(modifier = Modifier.size(112.dp)...)` for dimension changes, or the `val (buttonText, buttonSubtext, buttonColor) = when (state)` block to modify the text or color for any state.

### Example 2: Changing the Primary Brand Colors
1. Open `app/src/main/java/com/palash/voicebridge/ui/theme/Theme.kt`.
2. Locate `val PalashGreen = Color(0xFF1B5E20)`.
3. Changing this single value updates primary buttons, top bars, active chips, and title accents across all screens.

### Example 3: Modifying the "What you said" Card
1. Open `app/src/main/java/com/palash/voicebridge/ui/translator/LiveTranslatorScreen.kt`.
2. Locate `HindiTranscriptCard(hindiText: String)`.
3. Customize title, placeholder text, or typography.

### Example 4: Modifying the "Mother-tongue translation" Card
1. Open `app/src/main/java/com/palash/voicebridge/ui/translator/LiveTranslatorScreen.kt`.
2. Locate `MotherTongueTranslationCard(uiState: TranslatorUiState)`.
3. Customize Ol Chiki font size, pronunciation badge layout, or confidence score indicators.
