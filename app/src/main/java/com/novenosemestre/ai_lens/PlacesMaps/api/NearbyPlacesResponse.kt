package com.novenosemestre.ai_lens.PlacesMaps.api


import com.google.gson.annotations.SerializedName
import com.novenosemestre.ai_lens.PlacesMaps.model.Place

/**
 * Data class encapsulating a response from the nearby search call to the Places API.
 */
data class NearbyPlacesResponse(

   /**
     * The 'results' property represents a list of Place objects.
     * These objects are part of the response from the nearby search call to the Places API.
     * The @SerializedName annotation is a Gson annotation indicating this member should be serialized to JSON with the provided name value as its field name.
     *
     * @property results The list of Place objects returned from the Places API.
     */
    @SerializedName("results") val results: List<Place>

)