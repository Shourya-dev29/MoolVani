package com.palash.voicebridge.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for curriculum phrase database operations.
 * All database operations are suspend functions or return Flow for reactive UI.
 */
@Dao
interface CurriculumDao {

    // ─── Insert / Update ──────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: CurriculumEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrases(phrases: List<CurriculumEntity>)

    @Update
    suspend fun updatePhrase(phrase: CurriculumEntity)

    @Delete
    suspend fun deletePhrase(phrase: CurriculumEntity)

    // ─── Basic Queries ────────────────────────────────────────────────────────

    @Query("SELECT * FROM curriculum_phrases ORDER BY domain, topic, id")
    fun getAllPhrases(): Flow<List<CurriculumEntity>>

    @Query("SELECT * FROM curriculum_phrases ORDER BY domain, topic, id")
    suspend fun getAllPhrasesSync(): List<CurriculumEntity>

    @Query("SELECT COUNT(*) FROM curriculum_phrases")
    suspend fun getTotalCount(): Int

    @Query("SELECT * FROM curriculum_phrases WHERE id = :id")
    suspend fun getPhraseById(id: Long): CurriculumEntity?

    // ─── Search ───────────────────────────────────────────────────────────────

    /**
     * Full-text search across Hindi phrase, domain, and topic.
     */
    @Query("""
        SELECT * FROM curriculum_phrases 
        WHERE hindiPhrase LIKE '%' || :query || '%'
           OR domain LIKE '%' || :query || '%'
           OR topic LIKE '%' || :query || '%'
        ORDER BY 
            CASE WHEN hindiPhrase LIKE :query || '%' THEN 0 ELSE 1 END,
            domain, topic
        LIMIT 50
    """)
    suspend fun searchHindiPhrase(query: String): List<CurriculumEntity>

    /**
     * Exact phrase match (case-sensitive, normalized).
     */
    @Query("SELECT * FROM curriculum_phrases WHERE hindiPhrase = :phrase LIMIT 1")
    suspend fun findExactPhrase(phrase: String): CurriculumEntity?

    /**
     * Find phrases whose Hindi text starts with the given prefix.
     * Used as a fast pre-filter before fuzzy matching.
     */
    @Query("""
        SELECT * FROM curriculum_phrases 
        WHERE hindiPhrase LIKE :prefix || '%'
        LIMIT 20
    """)
    suspend fun findByPrefix(prefix: String): List<CurriculumEntity>

    /**
     * Returns all phrases for fuzzy matching in-memory.
     * Cached in TranslationEngine — do not call repeatedly.
     */
    @Query("SELECT * FROM curriculum_phrases WHERE domain = :domain")
    suspend fun getPhrasesByDomain(domain: String): List<CurriculumEntity>

    // ─── Filter Queries ───────────────────────────────────────────────────────

    @Query("SELECT * FROM curriculum_phrases WHERE domain = :domain ORDER BY topic, id")
    fun getByDomain(domain: String): Flow<List<CurriculumEntity>>

    @Query("SELECT * FROM curriculum_phrases WHERE topic = :topic ORDER BY difficulty, id")
    fun getByTopic(topic: String): Flow<List<CurriculumEntity>>

    @Query("SELECT * FROM curriculum_phrases WHERE verified = 1 ORDER BY domain, topic")
    fun getVerifiedPhrases(): Flow<List<CurriculumEntity>>

    @Query("SELECT * FROM curriculum_phrases WHERE difficulty = :difficulty ORDER BY domain, topic")
    fun getByDifficulty(difficulty: String): Flow<List<CurriculumEntity>>

    @Query("""
        SELECT * FROM curriculum_phrases 
        WHERE (:domain = '' OR domain = :domain)
          AND (:topic = '' OR topic = :topic)
          AND (:difficulty = '' OR difficulty = :difficulty)
          AND (:verifiedOnly = 0 OR verified = 1)
        ORDER BY domain, topic, id
    """)
    fun getFiltered(
        domain: String = "",
        topic: String = "",
        difficulty: String = "",
        verifiedOnly: Boolean = false
    ): Flow<List<CurriculumEntity>>

    // ─── Aggregation ─────────────────────────────────────────────────────────

    @Query("SELECT DISTINCT domain FROM curriculum_phrases ORDER BY domain")
    suspend fun getAllDomains(): List<String>

    @Query("SELECT DISTINCT topic FROM curriculum_phrases WHERE domain = :domain ORDER BY topic")
    suspend fun getTopicsForDomain(domain: String): List<String>

    @Query("SELECT COUNT(*) FROM curriculum_phrases WHERE santhaliTranslation IS NOT NULL")
    suspend fun getSanthaliCount(): Int

    @Query("SELECT COUNT(*) FROM curriculum_phrases WHERE verified = 1")
    suspend fun getVerifiedCount(): Int

    // ─── Seed ─────────────────────────────────────────────────────────────────

    @Query("DELETE FROM curriculum_phrases")
    suspend fun clearAll()
}
