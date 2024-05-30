package com.novenosemestre.ai_lens.PlacesMaps.api


import com.google.gson.annotations.SerializedName
import com.novenosemestre.ai_lens.PlacesMaps.model.Place

/**
 * Data class encapsulating a response from the nearby search call to the Places API.
 */
data class NearbyPlacesResponse(
    @SerializedName("results") val results: List<Place>
)