package com.example.tilawatchecker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AyahAdapter : RecyclerView.Adapter<AyahAdapter.ViewHolder>() {

    private var items: List<AyahEntity> = emptyList()

    fun submitList(list: List<AyahEntity>) {
        items = list
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ayahBadge: TextView = view.findViewById(R.id.ayahBadge)
        val ayahText: TextView = view.findViewById(R.id.ayahText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ayah, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.ayahBadge.text = item.ayahNumber.toString()
        holder.ayahText.text = item.textArabic
    }

    override fun getItemCount() = items.size
}
