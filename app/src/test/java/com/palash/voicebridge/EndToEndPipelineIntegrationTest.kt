package com.palash.voicebridge

import com.palash.voicebridge.data.local.CurriculumEntity
import com.palash.voicebridge.data.local.CurriculumSeed
import com.palash.voicebridge.domain.speech.SherpaSpeechRecognizer
import com.palash.voicebridge.domain.translation.HindiNormalizer
import com.palash.voicebridge.domain.translation.LevenshteinMatcher
import com.palash.voicebridge.domain.translation.MatchType
import com.palash.voicebridge.domain.translation.TargetLanguage
import com.palash.voicebridge.domain.translation.TranslationResult
import com.palash.voicebridge.domain.tts.SherpaTtsEngine
import com.palash.voicebridge.utils.LatencyTracker
import com.palash.voicebridge.utils.Script
import com.palash.voicebridge.utils.UnicodeUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * Logical End-to-End integration test suite for the PALASH VoiceBridge offline pipeline.
 * Tests normalization, exact matching, fuzzy matching, linguistic verification metadata,
 * safe hallucination rejection, and multi-trial latency benchmarking.
 */
class EndToEndPipelineIntegrationTest {

    @Test
    fun testEndToEnd_verifiedClassroomPhrasePipeline() {
        val rawInput = "  किताब खोलो।  "
        val normalized = HindiNormalizer.normalize(rawInput)
        assertEquals("किताब खोलो", normalized)

        // Seed data verification
        val seedPhrases = CurriculumSeed.getSeedPhrases()
        val match = seedPhrases.firstOrNull { it.hindiPhrase == normalized }
        assertNotNull("Must find 'किताब खोलो' in curriculum seed", match)
        assertEquals("ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ", match?.santhaliTranslation)
        assertTrue("Foundational classroom command must be verified", match?.verified == true)
        assertEquals("SCERT / Ol Chiki Primary Reader", match?.source)

        // Verify Ol Chiki script detection
        val script = UnicodeUtils.detectScript(match!!.santhaliTranslation!!)
        assertEquals(Script.OL_CHIKI, script)
    }

    @Test
    fun testEndToEnd_fuzzyMatchingMinorVariation() {
        val rawTeacherSpeech = "किताब खोल"
        val normalized = HindiNormalizer.normalize(rawTeacherSpeech)

        val seedPhrases = CurriculumSeed.getSeedPhrases()
        val candidates = seedPhrases.map { it.hindiPhrase to it }

        val bestMatch = LevenshteinMatcher.findBestMatch(normalized, candidates, threshold = 0.72f)
        assertNotNull("Should fuzzy-match minor ASR inflection", bestMatch)
        assertEquals("किताब खोलो", bestMatch?.match?.hindiPhrase)
        assertTrue("Similarity must be > 0.8", bestMatch!!.similarity > 0.8f)
    }

    @Test
    fun testEndToEnd_safeRejectionOfUnrelatedPhrase() {
        val unrelatedInput = "आज मौसम बहुत गर्म है और बारिश होगी"
        val normalized = HindiNormalizer.normalize(unrelatedInput)

        val seedPhrases = CurriculumSeed.getSeedPhrases()
        val candidates = seedPhrases.map { it.hindiPhrase to it }

        val bestMatch = LevenshteinMatcher.findBestMatch(normalized, candidates, threshold = 0.72f)
        assertNull("Unrelated educational phrase must NEVER match a classroom command", bestMatch)
    }

    @Test
    fun testLatencyTracker_multiTrialBenchmarking() {
        val tracker = LatencyTracker()

        // Trial 1: ~100ms
        tracker.markStart()
        Thread.sleep(50)
        tracker.mark(LatencyTracker.STAGE_ASR)
        Thread.sleep(50)
        tracker.markEnd()

        // Trial 2: ~150ms
        tracker.markStart()
        Thread.sleep(70)
        tracker.mark(LatencyTracker.STAGE_ASR)
        Thread.sleep(80)
        tracker.markEnd()

        val stats = tracker.getTrialStats()
        assertEquals(2, stats.count)
        assertTrue("Min should be around 100ms", stats.minMs in 80..180)
        assertTrue("Max should be >= min", stats.maxMs >= stats.minMs)
        assertTrue("Average should be between min and max", stats.avgMs in stats.minMs..stats.maxMs)

        val slowest = tracker.getSlowestStage()
        assertNotNull(slowest)
    }

    @Test
    fun testSherpaAdapters_gracefulHandlingWhenMissing() {
        // When running in unit test JVM without Android assets / AAR
        assertFalse("Runtime should report not on JVM unit test classpath", SherpaSpeechRecognizer.isRuntimeAvailable())
        assertFalse("TTS runtime should report not on JVM unit test classpath", SherpaTtsEngine.isRuntimeAvailable())
    }

    @Test
    fun testCurriculumSeed_hasBothVerifiedAndReviewEntries() {
        val phrases = CurriculumSeed.getSeedPhrases()
        assertTrue("Should have over 120 phrases", phrases.size >= 120)

        val verifiedCount = phrases.count { it.verified }
        val reviewCount = phrases.count { !it.verified }

        assertTrue("Must contain verified SCERT foundational words", verifiedCount >= 30)
        assertTrue("Must contain explicit demo/review required entries", reviewCount >= 30)
    }

    @Test
    fun testSantaliTtsModel_olChikiTokensCoverage() {
        // Read tokens.txt from assets
        val tokensFile = java.io.File("src/main/assets/models/tts/santali/tokens.txt")
        val altTokensFile = java.io.File("app/src/main/assets/models/tts/santali/tokens.txt")
        val targetFile = if (tokensFile.exists()) tokensFile else altTokensFile

        if (targetFile.exists()) {
            val validTokens = targetFile.readLines().mapNotNull { line ->
                val parts = line.split(" ")
                if (parts.isNotEmpty()) parts[0] else null
            }.toSet()

            // Verify that verified curriculum phrases contain valid Ol Chiki characters
            val seedPhrases = CurriculumSeed.getSeedPhrases().filter { it.verified && it.santhaliTranslation != null }
            for (phrase in seedPhrases) {
                val santali = phrase.santhaliTranslation!!
                for (ch in santali) {
                    if (ch != ' ' && ch != '-' && ch != '?' && ch != '!' && ch != '.') {
                        val str = ch.toString()
                        assertTrue(
                            "Character '$str' (U+${Integer.toHexString(ch.code).uppercase()}) in phrase '$santali' must be in Santali Piper TTS token vocabulary",
                            validTokens.contains(str)
                        )
                    }
                }
            }
        }
    }

    @Test
    fun testTtsResult_successAndPlaybackContract() {
        val mockSamples = ShortArray(1600) { (it % 100).toShort() }
        val result = com.palash.voicebridge.domain.tts.TtsResult(
            audioData = mockSamples,
            sampleRate = 16000,
            latencyMs = 120,
            isDemoMode = false
        )
        assertTrue(result.isSuccess)
        assertEquals(16000, result.sampleRate)
        assertNotNull(result.audioData)
        assertEquals(1600, result.audioData?.size)
    }
}
