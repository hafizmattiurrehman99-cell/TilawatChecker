package com.example.tilawatchecker

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads the full Quran (Arabic, Uthmani script) from a public, well-established
 * Quran API (alquran.cloud) on first run and stores it locally. After this one-time
 * download, the app works fully offline for browsing and recitation checking.
 *
 * We deliberately do NOT hardcode any Quran text ourselves - it is fetched from a
 * dedicated Quran data source so the text stays authentic and accurate.
 */
class QuranRepository(private val context: Context) {

    private val dao = QuranDatabase.getInstance(context).quranDao()

    suspend fun isDataDownloaded(): Boolean {
        return dao.surahCount() > 0
    }

    /** Returns null on success, or an error message on failure. */
    suspend fun downloadQuranData(): String? {
        return try {
            val url = URL("https://api.alquran.cloud/v1/quran/quran-uthmani")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 20000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                return "Server returned code $responseCode"
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            val data = root.getJSONObject("data")
            val surahsJson = data.getJSONArray("surahs")

            val surahList = mutableListOf<SurahEntity>()
            val ayahList = mutableListOf<AyahEntity>()

            for (i in 0 until surahsJson.length()) {
                val s = surahsJson.getJSONObject(i)
                val surahNumber = s.getInt("number")
                val nameArabic = s.getString("name")
                val nameEnglish = s.optString("englishName", "")
                val ayahsJson = s.getJSONArray("ayahs")

                surahList.add(
                    SurahEntity(
                        number = surahNumber,
                        nameArabic = nameArabic,
                        nameEnglish = nameEnglish,
                        ayahCount = ayahsJson.length()
                    )
                )

                for (j in 0 until ayahsJson.length()) {
                    val a = ayahsJson.getJSONObject(j)
                    val ayahNumber = a.getInt("numberInSurah")
                    val text = a.getString("text")
                    ayahList.add(
                        AyahEntity(
                            surahNumber = surahNumber,
                            ayahNumber = ayahNumber,
                            textArabic = text
                        )
                    )
                }
            }

            dao.insertSurahs(surahList)
            dao.insertAyahs(ayahList)
            null
        } catch (e: Exception) {
            e.message ?: "Unknown error while downloading Quran data"
        }
    }
}
