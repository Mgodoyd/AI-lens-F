package com.novenosemestre.ai_lens.ImageSearchHandler

import android.content.Context
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
class ImageSearchHandler(private val cx: String, private val apiKey: String, private val context: Context) {

   /**
     * This function is used to search for similar images using the Google Custom Search API.
     * It takes a search term and a callback function as arguments.
     *
     * The function first gets a list of e-commerce sites to search on and a list of start indexes for the search.
     * It then initializes a mutable list to store the search results and two counters for the total expected results and the received results.
     *
     * The function then iterates over each start index and each e-commerce site.
     * For each combination of start index and site, it creates a URL for the API request and a JSON object request.
     * The JSON object request is a GET request with the URL and null as the request body.
     *
     * The function handles the response from the API in the onResponse method of the JsonObjectRequest.
     * It parses the results from the response using the parseResults function and adds them to the allResults list.
     * It also increments the receivedResults counter by the number of results received.
     * If the number of received results is greater than or equal to the total expected results, it calls the onResults callback function with the allResults list as the argument.
     *
     * If the request fails, the function prints the stack trace of the error in the onErrorResponse method of the JsonObjectRequest.
     *
     * The function then adds the JSON object request to a request queue, which is created using the Volley library.
     */
    fun searchSimilarImages(searchTerm: String, onResults: (List<SearchResult>) -> Unit) {

        val ecommerceSites = listSitesSearch().ecommerceSites
        // Start indexes for each call
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

   /**
     * This function is used to parse the results from the Google Custom Search API response.
     * It takes a JSONObject as an argument, which is the response from the API.
     *
     * The function initializes a mutable list to store the search results.
     * It then gets the "items" JSONArray from the response.
     *
     * If the "items" JSONArray is not null, the function iterates over each item in the array.
     * For each item, it gets the title, image URL, page URL, and snippet.
     * The image URL is retrieved from the "pagemap" JSONObject, the "cse_thumbnail" JSONArray, and the first object in the array.
     * If the image URL is null, an empty string is used as a fallback.
     *
     * The function then creates a SearchResult with the title, image URL, page URL, and snippet, and adds it to the results list.
     *
     * The function returns the results list.
     */
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

   /**
     * Data class representing a search result from the Google Custom Search API.
     *
     * @property title The title of the search result, typically the title of the webpage.
     * @property imageUrl The URL of an image from the search result. This is typically a thumbnail image.
     * @property pageUrl The URL of the webpage of the search result.
     * @property snippet A short snippet of text from the webpage of the search result.
     */
    data class SearchResult(val title: String, val imageUrl: String, val pageUrl: String, val snippet: String)
}