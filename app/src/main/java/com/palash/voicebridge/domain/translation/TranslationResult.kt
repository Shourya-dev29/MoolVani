package com.palash.voicebridge.domain.translation

import com.palash.voicebridge.data.local.CurriculumEntity

/**
 * Translation result returned by TranslationEngine.
 *
 * IMPORTANT: matchType and verified fields must be surfaced in the UI
 * so users always know whether a translation is verified or a demo approximation.
 */
data class TranslationResult(
    /** Original input text (pre-normalization) */
    val sourceText: String,

    /** Normalized form used for matching */
    val normalizedSource: String,

    /** Translated text in the target language. Null if unavailable. */
    val translatedText: String?,

    /** Target language identifier */
    val targetLanguage: TargetLanguage,

    /** How the match was found */
    val matchType: MatchType,

    /** Confidence score 0.0–1.0 */
    val confidence: Float,

    /** Total pipeline latency in milliseconds */
    val latencyMs: Long,

    /** Whether translation was from a verified curriculum source */
    val verified: Boolean,

    /** Source of translation data */
    val source: String,

    /** The matched curriculum entity (for display details) */
    val curriculumEntity: CurriculumEntity? = null,

    /** Pronunciation hint (if available) */
    val pronunciation: String? = null,

    /** Error message if something went wrong */
    val errorMessage: String? = null
) {
    val isAvailable: Boolean get() = !translatedText.isNullOrBlank()
    val isVerified: Boolean get() = verified && isAvailable
}

enum class MatchType {
    /** Exact phrase found in curriculum database */
    EXACT,

    /** Fuzzy/approximate match found in curriculum database */
    FUZZY,

    /** Matched using ML/NLP inference (not yet implemented) */
    ML_FALLBACK,

    /** No match found — translation unavailable */
    UNAVAILABLE,

    /** Error during translation */
    ERROR
}

enum class TargetLanguage(
    val displayName: String,
    val code: String,
    val isAvailable: Boolean,
    val statusMessage: String
) {
    SANTHALI(
        displayName = "Santhali (ᱥᱟᱱᱛᱟᱲᱤ)",
        code = "sat",
        isAvailable = true,
        statusMessage = "Demo curriculum available"
    ),
    HO(
        displayName = "Ho (𑣙𑣉)",
        code = "hoc",
        isAvailable = false,
        statusMessage = "Architecture ready — model/content not installed"
    ),
    MUNDARI(
        displayName = "Mundari (मुंडारी)",
        code = "unr",
        isAvailable = false,
        statusMessage = "Architecture ready — model/content not installed"
    );

    companion object {
        fun fromCode(code: String): TargetLanguage? =
            entries.firstOrNull { it.code == code }
    }
}
