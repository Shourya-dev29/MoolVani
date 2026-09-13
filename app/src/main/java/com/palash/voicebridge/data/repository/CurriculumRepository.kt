package com.palash.voicebridge.data.repository

import com.palash.voicebridge.data.local.CurriculumDao
import com.palash.voicebridge.data.local.CurriculumEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for curriculum data access.
 * Abstracts the data layer from ViewModels.
 */
class CurriculumRepository(private val dao: CurriculumDao) {

    fun getAllPhrases(): Flow<List<CurriculumEntity>> = dao.getAllPhrases()

    suspend fun getAllPhrasesSync(): List<CurriculumEntity> = dao.getAllPhrasesSync()

    suspend fun searchPhrases(query: String): List<CurriculumEntity> =
        dao.searchHindiPhrase(query)

    suspend fun findExact(phrase: String): CurriculumEntity? =
        dao.findExactPhrase(phrase)

    fun getByDomain(domain: String): Flow<List<CurriculumEntity>> =
        dao.getByDomain(domain)

    fun getByTopic(topic: String): Flow<List<CurriculumEntity>> =
        dao.getByTopic(topic)

    fun getVerified(): Flow<List<CurriculumEntity>> =
        dao.getVerifiedPhrases()

    fun getFiltered(
        domain: String = "",
        topic: String = "",
        difficulty: String = "",
        verifiedOnly: Boolean = false
    ): Flow<List<CurriculumEntity>> = dao.getFiltered(domain, topic, difficulty, verifiedOnly)

    suspend fun getTotalCount(): Int = dao.getTotalCount()
    suspend fun getSanthaliCount(): Int = dao.getSanthaliCount()
    suspend fun getVerifiedCount(): Int = dao.getVerifiedCount()
    suspend fun getAllDomains(): List<String> = dao.getAllDomains()
    suspend fun getTopicsForDomain(domain: String): List<String> = dao.getTopicsForDomain(domain)

    suspend fun insertPhrase(phrase: CurriculumEntity): Long = dao.insertPhrase(phrase)
    suspend fun updatePhrase(phrase: CurriculumEntity) = dao.updatePhrase(phrase)
    suspend fun deletePhrase(phrase: CurriculumEntity) = dao.deletePhrase(phrase)
}
