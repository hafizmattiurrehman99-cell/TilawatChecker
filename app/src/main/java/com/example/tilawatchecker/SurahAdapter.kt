package com.example.tilawatchecker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SurahAdapter(
    private val onClick: (SurahEntity) -> Unit
) : RecyclerView.Adapter<SurahAdapter.ViewHolder>() {

    private var items: List<SurahEntity> = emptyList()

    fun submitList(list: List<SurahEntity>) {
        items = list
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view.findViewById(R.id.rootRow)
        val surahNumber: TextView = view.findViewById(R.id.surahNumber)
        val nameEnglish: TextView = view.findViewById(R.id.nameEnglish)
        val nameArabic: TextView = view.findViewById(R.id.nameArabic)
        val ayahCount: TextView = view.findViewById(R.id.ayahCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_surah, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.surahNumber.text = item.number.toString()
        holder.nameEnglish.text = item.nameEnglish
        holder.nameArabic.text = item.nameArabic
        holder.ayahCount.text = "${item.ayahCount} Ayahs"
        holder.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
