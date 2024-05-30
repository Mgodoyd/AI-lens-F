package com.novenosemestre.ai_lens.ImageSearchHandler

import android.annotation.SuppressLint
import android.app.appsearch.SearchResult
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.novenosemestre.ai_lens.R

class ResultAdapter(private var results: MutableList<ImageSearchHandler.SearchResult>) : RecyclerView.Adapter<ResultAdapter.ResultViewHolder>() {

    class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val snippetTextView: TextView = itemView.findViewById(R.id.snippetTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val result = results[position]

        holder.titleTextView.text = result.title
        holder.snippetTextView.text = result.snippet
        Glide.with(holder.itemView.context)
            .load(result.imageUrl)
            .into(holder.imageView)

        // Abrir la página en el navegador al hacer clic en el elemento
        holder.itemView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(result.pageUrl)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return results.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateResults(newResults: List<ImageSearchHandler.SearchResult>) {
        println("Updating results with: $newResults")
        results.clear()
        results.addAll(newResults)
        notifyDataSetChanged()
    }
}