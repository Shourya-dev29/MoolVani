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
    DEMO,
    MISSING,
    ERROR;

    val displayLabel: String get() = when (this) {
        READY -> "Ready"
        DEMO -> "Demo Mode"
        MISSING -> "Model Missing"
        ERROR -> "Error"
    }
}

class OfflineStatusManager(private val modelManager: ModelManager) {

    fun buildStatus(curriculumCount: Int): OfflineStatus {
        val asrStatus = if (modelManager.isHindiAsrReady)
            ComponentStatus.READY else ComponentStatus.DEMO
        val ttsStatus = if (modelManager.isSanthaliTtsReady)
            ComponentStatus.READY else ComponentStatus.DEMO

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
