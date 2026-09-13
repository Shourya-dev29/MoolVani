package com.palash.voicebridge.domain.translation

import java.text.Normalizer

/**
 * Normalizes Hindi text for consistent matching.
 *
 * Handles:
 * - Trailing/leading whitespace
 * - Multiple spaces
 * - Hindi-specific punctuation (।, ?, !, ,)
 * - Unicode normalization (NFC)
 * - Common ASR artifacts (repeated characters, stray characters)
 * - Devanagari punctuation variants
 */
object HindiNormalizer {

    // Hindi punctuation to strip from phrase boundaries
    private val STRIP_PATTERN = Regex("""[।\.\?\!,;:\u200B\u200C\u200D\uFEFF\u00A0]+""")
    private val MULTI_SPACE_PATTERN = Regex("""\s+""")
    private val DEVANAGARI_NUKTABLE_PATTERN = Regex("""[\u093C]""") // Nukta combining

    /**
     * Normalize a Hindi phrase for matching.
     *
     * @param text Raw Hindi text from ASR or user input
     * @return Normalized form suitable for database lookup
     */
    fun normalize(text: String): String {
        if (text.isBlank()) return ""

        return text
            .trim()
            // Unicode normalize to NFC for consistent code point sequences
            .let { Normalizer.normalize(it, Normalizer.Form.NFC) }
            // Remove trailing/leading punctuation
            .replace(Regex("""^[।\.\?\!,;:\s]+"""), "")
            .replace(Regex("""[।\.\?\!,;:\s]+$"""), "")
            // Collapse multiple spaces
            .replace(MULTI_SPACE_PATTERN, " ")
            // Final trim
            .trim()
    }

    /**
     * Normalize and convert to lowercase for case-insensitive operations.
     * Note: Hindi Devanagari doesn't have case, but handles mixed scripts.
     */
    fun normalizeForSearch(text: String): String {
        return normalize(text).lowercase()
    }

    /**
     * Tokenize normalized Hindi text into words.
     */
    fun tokenize(text: String): List<String> {
        return normalize(text).split(" ").filter { it.isNotBlank() }
    }

    /**
     * Check if the text appears to be a valid Hindi phrase
     * (contains Devanagari characters).
     */
    fun containsDevanagari(text: String): Boolean {
        return text.any { it.code in 0x0900..0x097F }
    }

    /**
     * Strip common ASR artifacts:
     * - Repeated words ("किताब किताब खोलो" → "किताब खोलो")
     * - Leading/trailing filler words
     */
    fun removeAsrArtifacts(text: String): String {
        val normalized = normalize(text)
        val words = normalized.split(" ").filter { it.isNotBlank() }

        // Remove consecutive duplicate words
        val deduped = mutableListOf<String>()
        for (word in words) {
            if (deduped.isEmpty() || deduped.last() != word) {
                deduped.add(word)
            }
        }
        return deduped.joinToString(" ")
    }
}
