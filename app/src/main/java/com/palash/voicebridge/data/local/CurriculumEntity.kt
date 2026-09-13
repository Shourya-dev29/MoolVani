package com.palash.voicebridge.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a bilingual curriculum phrase.
 * Primary language: Hindi. Target languages: Santhali, Ho, Mundari.
 *
 * verified = true  → phrase reviewed by a linguistic expert
 * verified = false → demo/prototype phrase, not for official classroom use
 */
@Entity(
    tableName = "curriculum_phrases",
    indices = [
        Index(value = ["hindiPhrase"]),
        Index(value = ["domain"]),
        Index(value = ["topic"]),
        Index(value = ["verified"])
    ]
)
data class CurriculumEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Hindi source phrase (normalized form) */
    val hindiPhrase: String,

    /**
     * Santhali translation (Ol Chiki script preferred, Devanagari fallback).
     * Null = no translation available.
     */
    val santhaliTranslation: String? = null,

    /**
     * Ho language translation.
     * Null = no translation available (architecture ready).
     */
    val hoTranslation: String? = null,

    /**
     * Mundari language translation.
     * Null = no translation available (architecture ready).
     */
    val mundariTranslation: String? = null,

    /** Educational domain: Literacy, Numeracy, Colors, etc. */
    val domain: String,

    /** Specific topic within domain: e.g., "Classroom Commands" */
    val topic: String,

    /** Difficulty level: BEGINNER, INTERMEDIATE, ADVANCED */
    val difficulty: String = "BEGINNER",

    /** Pronunciation hint for the Santhali translation (Latin script) */
    val pronunciation: String? = null,

    /** Asset name for associated image (in assets/images/) */
    val imageAsset: String? = null,

    /** Asset name for pre-recorded audio (in assets/audio/) */
    val audioAsset: String? = null,

    /**
     * Whether this phrase has been verified by a linguistic expert.
     * IMPORTANT: Always show this to users — never present unverified
     * translations as authoritative.
     */
    val verified: Boolean = false,

    /**
     * Data source: "demo", "SCERT", "expert_review", "community", etc.
     */
    val source: String = "demo"
)
