package com.example.tilawatchecker

enum class MistakeType { SKIPPED, WRONG }

data class MistakeEntry(
    val ayahNumber: Int,
    val word: String,
    val type: MistakeType
)
