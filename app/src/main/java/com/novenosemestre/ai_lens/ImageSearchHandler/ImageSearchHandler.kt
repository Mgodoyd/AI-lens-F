package com.novenosemestre.ai_lens.ImageSearchHandler
import android.app.appsearch.SearchResult
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream
class ImageSearchHandler(private val cx: String, private val apiKey: String, private val context: Context) {

    fun searchSimilarImages(searchTerm: String, onResults: (List<SearchResult>) -> Unit) {

        val ecommerceSites = listSitesSearch().ecommerceSites
        // Índices de inicio para cada llamada
        val startIndexes = listOf(41, 31, 21, 11, 1)

        val allResults = mutableListOf<SearchResult>()
        var totalExpectedResults = startIndexes.size * ecommerceSites.size
        var receivedResults = 0

        startIndexes.forEach { startIndex ->
            ecommerceSites.forEach { site ->
                val url = "https://www.googleapis.com/customsearch/v1?key=$apiKey&cx=$cx&q=$searchTerm&start=$startIndex&siteSearch=$site"
                println(url)
                val jsonObjectRequest = JsonObjectRequest(
                    Request.Method.GET, url, null,
                    { response ->
                        val results = parseResults(response)
                        allResults.addAll(results)
                        receivedResults += results.size
                        if (receivedResults >= totalExpectedResults) {
                            onResults(allResults)
                        }
                    },
                    { error ->
                        error.printStackTrace()
                    }
                )
                val queue = Volley.newRequestQueue(context)
                queue.add(jsonObjectRequest)
            }
        }
    }

    private fun parseResults(response: JSONObject): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val items = response.optJSONArray("items")
        items?.let {
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val title = item.getString("title")
                val imageUrl = item.optJSONObject("pagemap")?.optJSONArray("cse_thumbnail")?.optJSONObject(0)?.optString("src") ?: ""
                val pageUrl = item.getString("link")
                val snippet = item.getString("snippet")
                results.add(SearchResult(title, imageUrl, pageUrl, snippet))
            }
        }
        return results
    }

    data class SearchResult(val title: String, val imageUrl: String, val pageUrl: String, val snippet: String)
}