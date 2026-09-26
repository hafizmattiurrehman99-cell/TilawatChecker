package com.example.tilawatchecker

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface QuranDao {
    @Insert
    suspend fun insertSurahs(surahs: List<SurahEntity>)

    @Insert
    suspend fun insertAyahs(ayahs: List<AyahEntity>)

    @Query("SELECT COUNT(*) FROM surahs")
    suspend fun surahCount(): Int

    @Query("SELECT * FROM surahs ORDER BY number ASC")
    fun getAllSurahs(): LiveData<List<SurahEntity>>

    @Query("SELECT * FROM ayahs WHERE surahNumber = :surahNumber ORDER BY ayahNumber ASC")
    fun getAyahsForSurah(surahNumber: Int): LiveData<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE surahNumber = :surahNumber ORDER BY ayahNumber ASC")
    suspend fun getAyahsForSurahOnce(surahNumber: Int): List<AyahEntity>
}
