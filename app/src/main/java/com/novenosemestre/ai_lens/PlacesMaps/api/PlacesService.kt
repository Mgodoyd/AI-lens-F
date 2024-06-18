package com.novenosemestre.ai_lens.PlacesMaps.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface definition for a service that interacts with the Places API.
 *
 * @see [Place Search](https://developers.google.com/places/web-service/search)
 */
interface PlacesService {

    /**
     * This function is used to fetch nearby places from the Google Places API.
     * It is annotated with @GET which means it will send a GET request to the specified endpoint.
     * The endpoint is "nearbysearch/json", so the full URL will be "https://maps.googleapis.com/maps/api/place/nearbysearch/json".
     *
     * The function parameters are annotated with @Query. This means they will be added to the URL as query parameters.
     * For example, if apiKey is "abc", location is "1.0,2.0", radiusInMeters is 1000, and placeType is "restaurant",
     * the full URL will be "https://maps.googleapis.com/maps/api/place/nearbysearch/json?key=abc&location=1.0,2.0&radius=1000&type=restaurant".
     *
     * @param apiKey The API key to authenticate the request.
     * @param location The latitude and longitude around which to retrieve place information. This must be specified as latitude,longitude.
     * @param radiusInMeters Defines the distance (in meters) within which to bias place results. The maximum allowed radius is 50 000 meters.
     * @param placeType Restricts the results to places matching the specified type.
     * @return A Call object which can be used to send the request.
     */
    @GET("nearbysearch/json")
    fun nearbyPlaces(
        @Query("key") apiKey: String,
        @Query("location") location: String,
        @Query("radius") radiusInMeters: Int,
        @Query("type") placeType: String
    ): Call<NearbyPlacesResponse>


    /**
     * Companion object for the PlacesService interface.
     * It contains a constant for the root URL of the Places API and a factory method to create an instance of PlacesService.
     *
     * @property ROOT_URL The root URL of the Places API.
     */
    companion object {
        /**
         * The root URL of the Places API.
         */
        private const val ROOT_URL = "https://maps.googleapis.com/maps/api/place/"

        /**
         * Factory method to create an instance of PlacesService.
         * This method sets up an OkHttpClient with a logging interceptor, a Gson converter factory, and a Retrofit instance.
         * The logging interceptor logs the HTTP request and response data in the logcat, which can be useful for debugging.
         * The Gson converter factory is used to convert the JSON response from the API into Kotlin objects.
         * The Retrofit instance is set up with the root URL, the OkHttpClient, and the Gson converter factory.
         * Finally, the method uses the Retrofit instance to create and return an instance of PlacesService.
         *
         * @return An instance of PlacesService.
         */
        fun create(): PlacesService {
            val logger = HttpLoggingInterceptor()
            logger.level = HttpLoggingInterceptor.Level.BODY
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()
            val converterFactory = GsonConverterFactory.create()
            val retrofit = Retrofit.Builder()
                .baseUrl(ROOT_URL)
                .client(okHttpClient)
                .addConverterFactory(converterFactory)
                .build()
            return retrofit.create(PlacesService::class.java)
        }
    }
}