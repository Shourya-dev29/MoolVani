package com.palash.voicebridge.domain.translation

import com.palash.voicebridge.data.local.CurriculumEntity
import com.palash.voicebridge.data.local.CurriculumDao
import com.palash.voicebridge.utils.LatencyTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Core translation engine for PALASH VoiceBridge.
 *
 * Pipeline:
 *   Input Text
 *   → HindiNormalizer
 *   → Exact Match (Room DB)
 *   → Fuzzy Match (Levenshtein + cached corpus)
 *   → ML Fallback (not implemented — returns UNAVAILABLE)
 *   → "Translation not available"
 *
 * SAFETY: Never fabricates translations. Returns TranslationResult with
 * matchType=UNAVAILABLE and translatedText=null when no safe match exists.
 *
 * Corpus cache: Loaded lazily on first translation, held in memory.
 * Cleared via clearCache() to free memory.
 */
class TranslationEngine(
    private val dao: CurriculumDao
) {

    /** In-memory cache of (normalizedHindi → CurriculumEntity) for fast lookup */
    @Volatile
    private var corpusCache: List<Pair<String, CurriculumEntity>>? = null

    companion object {
        /** Fuzzy match threshold — phrases more different than this are rejected */
        const val FUZZY_THRESHOLD = 0.72f

        /** Min phrase length for fuzzy matching (avoid spurious short matches) */
        const val MIN_FUZZY_LENGTH = 3
    }

    /**
     * Translate a Hindi phrase to the target language.
     *
     * @param hindiText Raw Hindi text (from ASR or demo input)
     * @param targetLanguage Target language to translate to
     * @return TranslationResult with full pipeline metadata
     */
    suspend fun translate(
        hindiText: String,
        targetLanguage: TargetLanguage
    ): TranslationResult = withContext(Dispatchers.Default) {
        val tracker = LatencyTracker()
        tracker.markStart()

        val normalized = HindiNormalizer.normalize(
            HindiNormalizer.removeAsrArtifacts(hindiText)
        )

        // Guard: empty or too short
        if (normalized.length < 1) {
            tracker.markEnd()
            return@withContext unavailable(
                hindiText, normalized, targetLanguage, tracker.totalMs(),
                "Input text is empty after normalization"
            )
        }

        // Guard: language not available
        if (!targetLanguage.isAvailable) {
            tracker.markEnd()
            return@withContext TranslationResult(
                sourceText = hindiText,
                normalizedSource = normalized,
                translatedText = null,
                targetLanguage = targetLanguage,
                matchType = MatchType.UNAVAILABLE,
                confidence = 0f,
                latencyMs = tracker.totalMs(),
                verified = false,
                source = "none",
                errorMessage = targetLanguage.statusMessage
            )
        }

        // ── Step 1: Exact match ────────────────────────────────────────────────
        val exactMatch = dao.findExactPhrase(normalized)
        if (exactMatch != null) {
            val translation = getTranslationFor(exactMatch, targetLanguage)
            if (translation != null) {
                tracker.markEnd()
                return@withContext TranslationResult(
                    sourceText = hindiText,
                    normalizedSource = normalized,
                    translatedText = translation,
                    targetLanguage = targetLanguage,
                    matchType = MatchType.EXACT,
                    confidence = 1.0f,
                    latencyMs = tracker.totalMs(),
                    verified = exactMatch.verified,
                    source = exactMatch.source,
                    curriculumEntity = exactMatch,
                    pronunciation = exactMatch.pronunciation
                )
            }
        }

        // ── Step 2: Fuzzy match ───────────────────────────────────────────────
        if (normalized.length >= MIN_FUZZY_LENGTH) {
            val corpus = getCorpusCache()
            val fuzzyResult = LevenshteinMatcher.findBestMatch(
                query = normalized,
                candidates = corpus,
                threshold = FUZZY_THRESHOLD
            )

            if (fuzzyResult != null) {
                val entity = fuzzyResult.match
                val translation = getTranslationFor(entity, targetLanguage)
                if (translation != null) {
                    tracker.markEnd()
                    return@withContext TranslationResult(
                        sourceText = hindiText,
                        normalizedSource = normalized,
                        translatedText = translation,
                        targetLanguage = targetLanguage,
                        matchType = MatchType.FUZZY,
                        confidence = fuzzyResult.similarity,
                        latencyMs = tracker.totalMs(),
                        verified = entity.verified,
                        source = entity.source,
                        curriculumEntity = entity,
                        pronunciation = entity.pronunciation
                    )
                }
            }
        }

        // ── Step 3: ML Fallback (architecture placeholder) ───────────────────
        // When an offline ML model is integrated, insert here.
        // For now: fall through to UNAVAILABLE.

        // ── Step 4: Unavailable ──────────────────────────────────────────────
        tracker.markEnd()
        return@withContext unavailable(
            hindiText, normalized, targetLanguage, tracker.totalMs(), null
        )
    }

    /**
     * Preload the corpus cache for fuzzy matching.
     * Call this during app initialization to reduce first-translation latency.
     */
    suspend fun preloadCache() = withContext(Dispatchers.IO) {
        if (corpusCache == null) {
            getCorpusCache()
        }
    }

    /** Clear the in-memory corpus cache to free memory */
    fun clearCache() {
        corpusCache = null
    }

    /** Get the current corpus cache size */
    suspend fun getCacheSize(): Int = getCorpusCache().size

    // ─── Private helpers ──────────────────────────────────────────────────────

    private suspend fun getCorpusCache(): List<Pair<String, CurriculumEntity>> {
        return corpusCache ?: withContext(Dispatchers.IO) {
            val all = dao.getAllPhrasesSync()
            val cache = all.map { entity ->
                HindiNormalizer.normalize(entity.hindiPhrase) to entity
            }
            corpusCache = cache
            cache
        }
    }

    private fun getTranslationFor(
        entity: CurriculumEntity,
        language: TargetLanguage
    ): String? {
        return when (language) {
            TargetLanguage.SANTHALI -> entity.santhaliTranslation
            TargetLanguage.HO -> entity.hoTranslation
            TargetLanguage.MUNDARI -> entity.mundariTranslation
        }
    }

    private fun unavailable(
        sourceText: String,
        normalized: String,
        language: TargetLanguage,
        latencyMs: Long,
        errorMessage: String?
    ) = TranslationResult(
        sourceText = sourceText,
        normalizedSource = normalized,
        translatedText = null,
        targetLanguage = language,
        matchType = MatchType.UNAVAILABLE,
        confidence = 0f,
        latencyMs = latencyMs,
        verified = false,
        source = "none",
        errorMessage = errorMessage
    )
}
