package com.palash.voicebridge

import com.palash.voicebridge.domain.translation.HindiNormalizer
import com.palash.voicebridge.domain.translation.LevenshteinMatcher
import com.palash.voicebridge.utils.LatencyTracker
import org.junit.Assert.*
import org.junit.Test

class TranslationUnitTests {

    @Test
    fun testHindiNormalizer_stripsPunctuation() {
        val raw = "किताब खोलो।"
        val normalized = HindiNormalizer.normalize(raw)
        assertEquals("किताब खोलो", normalized)
    }

    @Test
    fun testHindiNormalizer_collapsesExtraSpaces() {
        val raw = "  किताब    खोलो   "
        val normalized = HindiNormalizer.normalize(raw)
        assertEquals("किताब खोलो", normalized)
    }

    @Test
    fun testHindiNormalizer_handlesQuestionMarksAndExclamation() {
        val raw = "कितने हैं?!।"
        val normalized = HindiNormalizer.normalize(raw)
        assertEquals("कितने हैं", normalized)
    }

    @Test
    fun testHindiNormalizer_removesAsrRepeatedArtifacts() {
        val raw = "किताब किताब खोलो"
        val cleaned = HindiNormalizer.removeAsrArtifacts(raw)
        assertEquals("किताब खोलो", cleaned)
    }

    @Test
    fun testHindiNormalizer_detectsDevanagari() {
        assertTrue(HindiNormalizer.containsDevanagari("किताब खोलो"))
        assertFalse(HindiNormalizer.containsDevanagari("kitab kholo"))
    }

    @Test
    fun testLevenshteinMatcher_identicalStrings() {
        val s = "किताब खोलो"
        val dist = LevenshteinMatcher.editDistance(s, s)
        assertEquals(0, dist)
        assertEquals(1.0f, LevenshteinMatcher.similarity(s, s), 0.001f)
    }

    @Test
    fun testLevenshteinMatcher_similarPhrase() {
        val query = "किताब खोल"
        val candidate = "किताब खोलो"
        val similarity = LevenshteinMatcher.similarity(query, candidate)
        assertTrue("Similarity should be high for one character difference", similarity > 0.8f)

        val candidates = listOf(candidate to "FOUND")
        val match = LevenshteinMatcher.findBestMatch(query, candidates, threshold = 0.75f)
        assertNotNull(match)
        assertEquals("FOUND", match?.match)
    }

    @Test
    fun testLevenshteinMatcher_unrelatedPhrase() {
        val query = "मौसम कैसा है"
        val candidate = "किताब खोलो"
        val similarity = LevenshteinMatcher.similarity(query, candidate)
        assertTrue("Similarity should be very low for unrelated phrase", similarity < 0.4f)

        val candidates = listOf(candidate to "FOUND")
        val match = LevenshteinMatcher.findBestMatch(query, candidates, threshold = 0.75f)
        assertNull("Unrelated phrase must not match", match)
    }

    @Test
    fun testLatencyTracker_measuresAccurateElapsedTime() {
        val tracker = LatencyTracker()
        tracker.markStart()
        Thread.sleep(50)
        tracker.mark(LatencyTracker.STAGE_ASR)
        Thread.sleep(50)
        tracker.markEnd()

        val elapsed = tracker.totalMs()
        assertTrue("Elapsed time should be at least 100ms", elapsed >= 90)
        assertTrue("Should be under target", tracker.meetsTarget(3000L))

        val stages = tracker.stageReport()
        assertTrue(stages.containsKey(LatencyTracker.STAGE_ASR))
    }
}
