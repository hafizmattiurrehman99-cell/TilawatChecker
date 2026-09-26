package com.example.tilawatchecker

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SurahActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SURAH_NUMBER = "extra_surah_number"
        const val EXTRA_SURAH_NAME = "extra_surah_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_surah)

        val surahNumber = intent.getIntExtra(EXTRA_SURAH_NUMBER, 1)
        val surahName = intent.getStringExtra(EXTRA_SURAH_NAME) ?: ""

        findViewById<TextView>(R.id.toolbarTitle).text = surahName
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val adapter = AyahAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        QuranDatabase.getInstance(this).quranDao().getAyahsForSurah(surahNumber)
            .observe(this) { list -> adapter.submitList(list) }

        findViewById<android.widget.Button>(R.id.btnStartReciting).setOnClickListener {
            val i = Intent(this, RecitationActivity::class.java)
            i.putExtra(RecitationActivity.EXTRA_SURAH_NUMBER, surahNumber)
            i.putExtra(RecitationActivity.EXTRA_SURAH_NAME, surahName)
            startActivity(i)
        }
    }
}
