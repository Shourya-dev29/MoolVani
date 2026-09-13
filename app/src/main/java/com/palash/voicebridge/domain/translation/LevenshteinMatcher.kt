package com.palash.voicebridge.domain.translation

/**
 * Levenshtein edit-distance based fuzzy string matcher.
 *
 * Used in the translation pipeline to find near-matches when
 * an exact match is not found in the curriculum database.
 *
 * Threshold: configurable. Default = 0.75 similarity ratio.
 * Below threshold → UNAVAILABLE.
 */
object LevenshteinMatcher {

    /** Minimum similarity ratio to accept a fuzzy match (0.0–1.0) */
    const val DEFAULT_SIMILARITY_THRESHOLD = 0.75f

    /**
     * Compute Levenshtein edit distance between two strings.
     * Uses dynamic programming with O(min(m,n)) space.
     *
     * @return Number of single-character edits (insert/delete/replace)
     */
    fun editDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val m = s1.length
        val n = s2.length

        // Use two rows to save memory
        var prevRow = IntArray(n + 1) { it }
        var currRow = IntArray(n + 1)

        for (i in 1..m) {
            currRow[0] = i
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                currRow[j] = minOf(
                    currRow[j - 1] + 1,     // insertion
                    prevRow[j] + 1,          // deletion
                    prevRow[j - 1] + cost    // substitution
                )
            }
            // Swap rows
            val temp = prevRow
            prevRow = currRow
            currRow = temp
        }

        return prevRow[n]
    }

    /**
     * Compute normalized similarity: 1.0 = identical, 0.0 = completely different.
     *
     * similarity = 1 - (editDistance / maxLength)
     */
    fun similarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0f
        val distance = editDistance(s1, s2)
        return 1.0f - (distance.toFloat() / maxLen.toFloat())
    }

    /**
     * Find the best match for [query] among [candidates].
     *
     * @param query Normalized Hindi phrase to find
     * @param candidates List of (normalizedPhrase, originalObject) pairs
     * @param threshold Minimum similarity to accept (0.0–1.0)
     * @return Best match result or null if no match exceeds threshold
     */
    fun <T> findBestMatch(
        query: String,
        candidates: List<Pair<String, T>>,
        threshold: Float = DEFAULT_SIMILARITY_THRESHOLD
    ): FuzzyMatchResult<T>? {
        if (candidates.isEmpty()) return null

        var bestScore = 0f
        var bestCandidate: T? = null
        var bestPhrase = ""

        for ((phrase, candidate) in candidates) {
            val score = similarity(query, phrase)
            if (score > bestScore) {
                bestScore = score
                bestCandidate = candidate
                bestPhrase = phrase
            }
        }

        return if (bestScore >= threshold && bestCandidate != null) {
            FuzzyMatchResult(
                match = bestCandidate,
                matchedPhrase = bestPhrase,
                similarity = bestScore
            )
        } else {
            null
        }
    }

    /**
     * Find all matches above threshold, sorted by similarity descending.
     */
    fun <T> findAllMatches(
        query: String,
        candidates: List<Pair<String, T>>,
        threshold: Float = DEFAULT_SIMILARITY_THRESHOLD,
        maxResults: Int = 5
    ): List<FuzzyMatchResult<T>> {
        return candidates
            .map { (phrase, candidate) ->
                FuzzyMatchResult(
                    match = candidate,
                    matchedPhrase = phrase,
                    similarity = similarity(query, phrase)
                )
            }
            .filter { it.similarity >= threshold }
            .sortedByDescending { it.similarity }
            .take(maxResults)
    }

    data class FuzzyMatchResult<T>(
        val match: T,
        val matchedPhrase: String,
        val similarity: Float
    )
}
