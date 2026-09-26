package com.example.tilawatchecker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val statusText = findViewById<TextView>(R.id.statusText)
        val btnRetry = findViewById<Button>(R.id.btnRetry)

        btnRetry.setOnClickListener {
            btnRetry.visibility = View.GONE
            checkAndDownload()
        }

        checkAndDownload()
    }

    private fun checkAndDownload() {
        val statusText = findViewById<TextView>(R.id.statusText)
        val btnRetry = findViewById<Button>(R.id.btnRetry)
        val repository = QuranRepository(this)

        lifecycleScope.launch {
            val alreadyDownloaded = withContext(Dispatchers.IO) { repository.isDataDownloaded() }
            if (alreadyDownloaded) {
                goToMain()
                return@launch
            }

            statusText.text = "Downloading Quran text (first time only)..."
            val error = withContext(Dispatchers.IO) { repository.downloadQuranData() }

            if (error == null) {
                statusText.text = "Done!"
                goToMain()
            } else {
                statusText.text = "Could not download Quran data.\nCheck your internet connection.\n\nError: $error"
                btnRetry.visibility = View.VISIBLE
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
