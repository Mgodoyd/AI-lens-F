package com.novenosemestre.ai_lens.ImageSearchHandler

import android.annotation.SuppressLint
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

    /**
     * A ViewHolder provides a direct reference to the views that make up an item in the RecyclerView.
     * It's a part of the adapter design pattern for the RecyclerView.
     *
     * @property itemView The root view of the item layout. This is typically the CardView that contains the item.
     * @property imageView The ImageView that displays the image of the search result.
     * @property titleTextView The TextView that displays the title of the search result.
     * @property snippetTextView The TextView that displays the snippet of the search result.
     */
    class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val snippetTextView: TextView = itemView.findViewById(R.id.snippetTextView)
    }

    /**
     * This function is part of the RecyclerView.Adapter lifecycle and is used to create a new ViewHolder.
     * The ViewHolder holds the view for each item in the RecyclerView.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        // Inflate the item layout from XML using the parent context and create a ViewHolder with it
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_result, parent, false)
        return ResultViewHolder(view)
    }

    /**
     * This function is part of the RecyclerView.Adapter lifecycle and is used to bind the data to the ViewHolder.
     * This function is called by RecyclerView to display the data at the specified position.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     *
     * Inside this function:
     * - The function first retrieves the search result at the given position.
     * - It then sets the title and snippet of the search result to the corresponding TextViews in the ViewHolder.
     * - It uses the Glide library to load the image from the image URL of the search result into the ImageView in the ViewHolder.
     * - It sets an OnClickListener on the itemView of the ViewHolder. When the itemView is clicked, it opens the webpage of the search result in the browser.
     */
    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val result = results[position]

        holder.titleTextView.text = result.title
        holder.snippetTextView.text = result.snippet
        Glide.with(holder.itemView.context)
            .load(result.imageUrl)
            .into(holder.imageView)

        // Open the page in the browser when the item is clicked
        holder.itemView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(result.pageUrl)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

   /**
    * This function is part of the RecyclerView.Adapter lifecycle and is used to get the size of the data set.
    * In this case, it returns the size of the 'results' list which contains the search results.
    *
    * @return The total number of items in the data set held by the adapter.
    */
   override fun getItemCount(): Int {
       return results.size
   }

   /**
    * This function is used to update the 'results' list with new search results.
    * It first prints the new results to the console for debugging purposes.
    * Then it clears the 'results' list and adds all the new results to it.
    * Finally, it notifies the RecyclerView that the data set has changed, causing it to re-render the list.
    *
    * @param newResults The new list of search results to be added to the 'results' list.
    *
    * @SuppressLint("NotifyDataSetChanged") is used to suppress the lint warning for calling notifyDataSetChanged().
    * notifyDataSetChanged() is a method in the RecyclerView.Adapter class. It signals that the data has changed and any View reflecting the data set should refresh itself.
    */
   @SuppressLint("NotifyDataSetChanged")
   fun updateResults(newResults: List<ImageSearchHandler.SearchResult>) {
       println("Updating results with: $newResults")
       results.clear()
       results.addAll(newResults)
       notifyDataSetChanged()
   }
}