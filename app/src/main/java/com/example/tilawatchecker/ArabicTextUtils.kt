package com.example.tilawatchecker

object ArabicTextUtils {

    // Arabic diacritics (harakat/tashkeel) unicode range, plus tatweel (elongation mark)
    private val diacriticsRegex = Regex("[\u064B-\u0652\u0670\u0640]")

    /**
     * Normalizes Arabic text so that recognized speech (which has no diacritics)
     * can be fairly compared against the Quran text (which does have diacritics).
     * Also unifies different Alef/Yeh/Teh-marbuta forms that speech recognizers
     * commonly simplify.
     */
    fun normalize(text: String): String {
        var result = text
        result = diacriticsRegex.replace(result, "")
        result = result.replace(Regex("[إأآا]"), "ا")
        result = result.replace(Regex("[ىي]"), "ي")
        result = result.replace("ة", "ه")
        result = result.replace("ؤ", "و")
        result = result.replace("ئ", "ي")
        result = result.replace(Regex("[^\\p{L}\\p{N}\\s]"), "") // strip punctuation
        result = result.trim().replace(Regex("\\s+"), " ")
        return result
    }

    fun splitWords(text: String): List<String> {
        return normalize(text).split(" ").filter { it.isNotBlank() }
    }
}
