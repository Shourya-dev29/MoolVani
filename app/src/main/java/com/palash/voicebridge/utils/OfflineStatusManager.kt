package com.palash.voicebridge.utils

import com.palash.voicebridge.models.AppMode
import com.palash.voicebridge.models.ModelManager
import com.palash.voicebridge.models.ModelStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Provides a unified view of the app's offline readiness status.
 * Used by the home screen and status indicators throughout the UI.
 */
data class OfflineStatus(
    val networkRequired: Boolean = false,
    val curriculumReady: Boolean = false,
    val curriculumPhraseCount: Int = 0,
    val asrStatus: ComponentStatus = ComponentStatus.DEMO,
    val ttsStatus: ComponentStatus = ComponentStatus.DEMO,
    val translationStatus: ComponentStatus = ComponentStatus.READY,
    val appMode: AppMode = AppMode.DEMO
) {
    val isFullyOfflineReady: Boolean get() = curriculumReady && !networkRequired

    val overallStatusLabel: String get() = when {
        !curriculumReady -> "INITIALIZING..."
        appMode == AppMode.LIVE_AI -> "● LIVE AI READY"
        else -> "● OFFLINE READY (Demo Mode)"
    }
}

enum class ComponentStatus {
    READY,
    INSTALLED,
    DEMO,
    MISSING,
    ERROR;

    val displayLabel: String get() = when (this) {
        READY -> "Ready"
        INSTALLED -> "Installed — not ready"
        DEMO -> "Demo Mode"
        MISSING -> "Model Missing"
        ERROR -> "Error"
    }
}

class OfflineStatusManager(private val modelManager: ModelManager) {

    fun buildStatus(curriculumCount: Int): OfflineStatus {
        val asrModelState = modelManager.getModelState("hindi_asr")?.status
        val asrStatus = when (asrModelState) {
            ModelStatus.READY -> ComponentStatus.READY
            ModelStatus.INSTALLED -> ComponentStatus.INSTALLED
            ModelStatus.MISSING -> ComponentStatus.MISSING
            else -> ComponentStatus.DEMO
        }

        val ttsModelState = modelManager.getModelState("santhali_tts")?.status
        val ttsStatus = when (ttsModelState) {
            ModelStatus.READY -> ComponentStatus.READY
            ModelStatus.INSTALLED -> ComponentStatus.INSTALLED
            ModelStatus.MISSING -> ComponentStatus.MISSING
            else -> ComponentStatus.DEMO
        }

        return OfflineStatus(
            networkRequired = false,
            curriculumReady = curriculumCount > 0,
            curriculumPhraseCount = curriculumCount,
            asrStatus = asrStatus,
            ttsStatus = ttsStatus,
            translationStatus = if (curriculumCount > 0) ComponentStatus.READY else ComponentStatus.MISSING,
            appMode = modelManager.appMode
        )
    }
}
