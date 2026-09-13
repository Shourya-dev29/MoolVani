package com.palash.voicebridge.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * PALASH VoiceBridge Room database.
 *
 * Database name: nipun_fln_curriculum.db
 * Version: 1
 *
 * Tables:
 *  - curriculum_phrases  (CurriculumEntity)
 */
@Database(
    entities = [CurriculumEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun curriculumDao(): CurriculumDao

    companion object {
        const val DATABASE_NAME = "nipun_fln_curriculum.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed curriculum data on first creation
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.let { database ->
                                val seedData = CurriculumSeed.getSeedPhrases()
                                database.curriculumDao().insertPhrases(seedData)
                            }
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
