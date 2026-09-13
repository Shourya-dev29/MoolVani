package com.palash.voicebridge.models

/**
 * Information about an AI model managed by ModelManager.
 */
data class ModelInfo(
    val id: String,
    val displayName: String,
    val description: String,
    val language: String,
    val modelType: ModelType,
    val assetPath: String,
    val expectedSizeMb: Float,
    val version: String = "1.0",
    val checksum: String? = null
)

enum class ModelType {
    ASR,   // Automatic Speech Recognition
    TTS    // Text-to-Speech
}

enum class ModelStatus {
    /** Model files found and verified */
    INSTALLED,

    /** Model files not found in assets */
    MISSING,

    /** Model is currently being loaded into memory */
    LOADING,

    /** Model loaded and ready for inference */
    READY,

    /** Error during loading or inference */
    ERROR
}

data class ModelState(
    val info: ModelInfo,
    val status: ModelStatus,
    val errorMessage: String? = null,
    val loadedSizeMb: Float = 0f
)
