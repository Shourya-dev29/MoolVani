package com.palash.voicebridge

import com.palash.voicebridge.domain.translation.MatchType
import com.palash.voicebridge.domain.translation.TargetLanguage
import com.palash.voicebridge.domain.translation.TranslationResult
import com.palash.voicebridge.models.ModelInfo
import com.palash.voicebridge.models.ModelManager
import com.palash.voicebridge.models.ModelType
import org.junit.Assert.*
import org.junit.Test

class ModelAndLanguageUnitTests {

    @Test
    fun testTargetLanguages_santhaliIsPrimaryAvailable() {
        val santhali = TargetLanguage.SANTHALI
        assertTrue(santhali.isAvailable)
        assertEquals("sat", santhali.code)

        val ho = TargetLanguage.HO
        assertFalse("Ho is secondary/architecture-ready", ho.isAvailable)

        val mundari = TargetLanguage.MUNDARI
        assertFalse("Mundari is secondary/architecture-ready", mundari.isAvailable)
    }

    @Test
    fun testTranslationResult_availabilityChecks() {
        val resultAvailable = TranslationResult(
            sourceText = "किताब खोलो",
            normalizedSource = "किताब खोलो",
            translatedText = "kitab dalar",
            targetLanguage = TargetLanguage.SANTHALI,
            matchType = MatchType.EXACT,
            confidence = 1.0f,
            latencyMs = 120,
            verified = true,
            source = "SCERT"
        )
        assertTrue(resultAvailable.isAvailable)
        assertTrue(resultAvailable.isVerified)

        val resultUnavailable = TranslationResult(
            sourceText = "अज्ञात वाक्य",
            normalizedSource = "अज्ञात वाक्य",
            translatedText = null,
            targetLanguage = TargetLanguage.SANTHALI,
            matchType = MatchType.UNAVAILABLE,
            confidence = 0f,
            latencyMs = 45,
            verified = false,
            source = "none"
        )
        assertFalse(resultUnavailable.isAvailable)
        assertFalse(resultUnavailable.isVerified)
    }

    @Test
    fun testModelManager_knownModelsList() {
        val models = ModelManager.KNOWN_MODELS
        assertTrue(models.any { it.id == "hindi_asr" && it.modelType == ModelType.ASR })
        assertTrue(models.any { it.id == "santhali_tts" && it.modelType == ModelType.TTS })
        assertTrue(models.any { it.id == "ho_tts" && it.modelType == ModelType.TTS })
        assertTrue(models.any { it.id == "mundari_tts" && it.modelType == ModelType.TTS })
    }
}
