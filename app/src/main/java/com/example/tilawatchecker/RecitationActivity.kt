package com.example.tilawatchecker

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class RecitationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SURAH_NUMBER = "extra_surah_number"
        const val EXTRA_SURAH_NAME = "extra_surah_name"
        private const val MIC_PERMISSION_CODE = 101
        private const val LOOKAHEAD = 5

        private const val STATUS_PENDING = 0
        private const val STATUS_CORRECT = 1
        private const val STATUS_WRONG = 2
        private const val STATUS_SKIPPED = 3
    }

    private lateinit var ayahText: TextView
    private lateinit var statusText: TextView
    private lateinit var btnMic: ImageView
    private lateinit var btnStop: android.widget.Button
    private lateinit var btnMistakes: TextView

    private var ayahs: List<AyahEntity> = emptyList()
    private var currentAyahIndex = 0
    private var expectedWords: List<String> = emptyList()
    private var displayWords: List<String> = emptyList()
    private var wordStatus: IntArray = IntArray(0)
    private var matchedCount = 0

    private val mistakesList = mutableListOf<MistakeEntry>()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var surahNumber = 1
    private var surahName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recitation)

        surahNumber = intent.getIntExtra(EXTRA_SURAH_NUMBER, 1)
        surahName = intent.getStringExtra(EXTRA_SURAH_NAME) ?: ""

        ayahText = findViewById(R.id.ayahText)
        statusText = findViewById(R.id.statusText)
        btnMic = findViewById(R.id.btnMic)
        btnStop = findViewById(R.id.btnStop)
        btnMistakes = findViewById(R.id.btnMistakes)

        findViewById<TextView>(R.id.toolbarTitle).text = surahName
        findViewById<View>(R.id.btnBack).setOnClickListener {
            stopListening()
            finish()
        }

        btnMic.setOnClickListener { onMicTapped() }
        btnStop.setOnClickListener { stopListening() }
        btnMistakes.setOnClickListener { showMistakesDialog() }

        lifecycleScope.launch {
            ayahs = QuranDatabase.getInstance(this@RecitationActivity).quranDao()
                .getAyahsForSurahOnce(surahNumber)
            if (ayahs.isNotEmpty()) {
                loadAyah(0)
            } else {
                statusText.text = "No ayahs found for this Surah."
            }
        }
    }

    private fun loadAyah(index: Int) {
        currentAyahIndex = index
        matchedCount = 0
        val ayah = ayahs[index]
        displayWords = ayah.textArabic.trim().split(Regex("\\s+"))
        expectedWords = ArabicTextUtils.splitWords(ayah.textArabic)
        wordStatus = IntArray(expectedWords.size) { STATUS_PENDING }
        findViewById<TextView>(R.id.toolbarTitle).text = "$surahName  •  Ayah ${ayah.ayahNumber}/${ayahs.size}"
        renderAyahHighlighting()
    }

    private fun renderAyahHighlighting() {
        val fullText = displayWords.joinToString(" ")
        val spannable = SpannableString(fullText)

        var searchStart = 0
        for ((index, word) in displayWords.withIndex()) {
            val wordStart = fullText.indexOf(word, searchStart)
            if (wordStart == -1) continue
            val wordEnd = wordStart + word.length
            searchStart = wordEnd

            val status = wordStatus.getOrElse(index) { STATUS_PENDING }
            when (status) {
                STATUS_CORRECT -> spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this, R.color.correct_word)),
                    wordStart, wordEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                STATUS_WRONG -> spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this, R.color.wrong_word)),
                    wordStart, wordEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                STATUS_SKIPPED -> {
                    spannable.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(this, R.color.skipped_word)),
                        wordStart, wordEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannable.setSpan(StrikethroughSpan(), wordStart, wordEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                STATUS_PENDING -> {
                    if (index == matchedCount) {
                        spannable.setSpan(StyleSpan(Typeface.BOLD), wordStart, wordEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }
        }
        ayahText.text = spannable
    }

    private fun onMicTapped() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), MIC_PERMISSION_CODE
            )
            return
        }
        startListening()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MIC_PERMISSION_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        } else {
            Toast.makeText(this, "Microphone permission is needed to check recitation", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusText.text = "Speech recognition is not available on this device."
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    statusText.text = "Listening..."
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    statusText.text = "Processing..."
                }

                override fun onError(error: Int) {
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            // Just silence/no speech detected - normal during pauses, keep listening
                        }
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                            statusText.text = "No internet connection - recitation checking needs internet."
                        }
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            statusText.text = "Microphone permission missing."
                        }
                        else -> {
                            statusText.text = "Listening error (code $error) - retrying..."
                        }
                    }
                    if (isListening) {
                        restartListening()
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val bestMatch = matches?.firstOrNull()
                    if (!bestMatch.isNullOrBlank()) {
                        processRecognizedSpeech(bestMatch)
                    }
                    if (isListening) {
                        restartListening()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        isListening = true
        btnMic.visibility = View.GONE
        btnStop.visibility = View.VISIBLE
        launchRecognizerIntent()
    }

    private fun restartListening() {
        launchRecognizerIntent()
    }

    private fun launchRecognizerIntent() {
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            // Most devices don't have an offline Arabic pack installed, which made
            // recognition silently fail. Online recognition (needs internet) is far
            // more reliable for Arabic, so we no longer force offline mode.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            statusText.text = "Could not start listening: ${e.message}"
        }
    }

    private fun stopListening() {
        isListening = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        btnMic.visibility = View.VISIBLE
        btnStop.visibility = View.GONE
        statusText.text = "Tap the mic to start"
    }

    /**
     * Compares recognized speech against the expected ayah word-by-word.
     * Uses a small lookahead so that a WORD YOU SKIP is correctly detected
     * (rather than throwing off every following word's comparison).
     */
    private fun processRecognizedSpeech(spokenText: String) {
        val spokenWords = ArabicTextUtils.splitWords(spokenText)

        for (spokenWord in spokenWords) {
            if (matchedCount >= expectedWords.size) {
                moveToNextAyah()
                if (matchedCount >= expectedWords.size) break // Surah finished
            }

            val windowEnd = minOf(matchedCount + LOOKAHEAD, expectedWords.size)
            var foundAt = -1
            for (j in matchedCount until windowEnd) {
                if (isCloseEnough(spokenWord, expectedWords[j])) {
                    foundAt = j
                    break
                }
            }

            if (foundAt == -1) {
                // Word doesn't match anything nearby - likely mispronounced/misheard
                markWrong(matchedCount)
                matchedCount++
            } else {
                // Any words between matchedCount and foundAt were skipped over
                for (skippedIdx in matchedCount until foundAt) {
                    markSkipped(skippedIdx)
                }
                markCorrect(foundAt)
                matchedCount = foundAt + 1
            }
        }
        renderAyahHighlighting()
        updateMistakesChip()
    }

    private fun markCorrect(index: Int) {
        if (index in wordStatus.indices) wordStatus[index] = STATUS_CORRECT
    }

    private fun markWrong(index: Int) {
        if (index in wordStatus.indices) {
            wordStatus[index] = STATUS_WRONG
            mistakesList.add(MistakeEntry(ayahs[currentAyahIndex].ayahNumber, displayWords[index], MistakeType.WRONG))
            playTone(false)
        }
    }

    private fun markSkipped(index: Int) {
        if (index in wordStatus.indices) {
            wordStatus[index] = STATUS_SKIPPED
            mistakesList.add(MistakeEntry(ayahs[currentAyahIndex].ayahNumber, displayWords[index], MistakeType.SKIPPED))
            playTone(true)
        }
    }

    private fun updateMistakesChip() {
        btnMistakes.text = "Mistakes: ${mistakesList.size}"
    }

    private fun showMistakesDialog() {
        if (mistakesList.isEmpty()) {
            Toast.makeText(this, "No mistakes yet - ماشاء الله!", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = mistakesList.map {
            val kind = if (it.type == MistakeType.SKIPPED) "Skipped" else "Check pronunciation"
            "Ayah ${it.ayahNumber} • $kind"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Mistakes (${mistakesList.size})")
            .setItems(labels) { _, which -> showMistakeDetail(mistakesList[which]) }
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showMistakeDetail(entry: MistakeEntry) {
        val message = if (entry.type == MistakeType.SKIPPED) {
            "You skipped this word in Ayah ${entry.ayahNumber}:\n\n${entry.word}"
        } else {
            "This word in Ayah ${entry.ayahNumber} may need a pronunciation check:\n\n${entry.word}"
        }
        AlertDialog.Builder(this)
            .setTitle("Ayah ${entry.ayahNumber}")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    /** Small tolerance for minor speech-recognition spelling differences. */
    private fun isCloseEnough(a: String, b: String): Boolean {
        if (a == b) return true
        if (kotlin.math.abs(a.length - b.length) > 2) return false
        val distance = levenshtein(a, b)
        return distance <= 1
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
                else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
            }
        }
        return dp[a.length][b.length]
    }

    private fun moveToNextAyah() {
        if (currentAyahIndex < ayahs.size - 1) {
            loadAyah(currentAyahIndex + 1)
        } else {
            statusText.text = "Surah complete! 🎉"
            stopListening()
        }
    }

    private fun playTone(isSkip: Boolean) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
            val tone = if (isSkip) ToneGenerator.TONE_PROP_NACK else ToneGenerator.TONE_PROP_BEEP2
            toneGen.startTone(tone, 200)
        } catch (e: Exception) {
            // Ignore if tone generation fails on some devices
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
    }
}
