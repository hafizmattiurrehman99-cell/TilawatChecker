package com.example.tilawatchecker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val adapter = SurahAdapter { surah ->
            val i = Intent(this, SurahActivity::class.java)
            i.putExtra(SurahActivity.EXTRA_SURAH_NUMBER, surah.number)
            i.putExtra(SurahActivity.EXTRA_SURAH_NAME, surah.nameEnglish)
            startActivity(i)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        QuranDatabase.getInstance(this).quranDao().getAllSurahs()
            .observe(this) { list -> adapter.submitList(list) }
    }
}
