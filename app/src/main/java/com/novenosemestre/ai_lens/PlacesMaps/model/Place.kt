package com.novenosemestre.ai_lens.PlacesMaps.model

import com.google.android.gms.maps.model.LatLng
import com.google.ar.sceneform.math.Vector3
import com.google.maps.android.ktx.utils.sphericalHeading
import kotlin.math.cos
import kotlin.math.sin

/**
 * A model describing details about a Place (location, name, type, etc.).
 */
    data class Place(
        val id: String,
        val icon: String,
        val name: String,
        val geometry: Geometry
    ) {
    /**
     * Overrides the equals function for the Place class.
     *
     * This function is used to compare if the current instance of Place is equal to the 'other' object.
     * The function first checks if the 'other' object is an instance of Place. If it's not, the function returns false.
     * Then it compares the 'id' property of the current instance with the 'id' property of the 'other' object.
     * If the 'id' properties are equal, the function returns true, otherwise, it returns false.
     *
     * @param other The object to compare with the current instance.
     * @return A Boolean value indicating whether the current instance is equal to the 'other' object.
     */
    override fun equals(other: Any?): Boolean {
        if (other !is Place) {
            return false
        }
        return this.id == other.id
    }

    /**
     * Overrides the hashCode function for the Place class.
     *
     * This function is used to generate a hash code for the current instance of Place.
     * The function returns the hash code of the 'id' property of the current instance.
     * This hash code can be used for operations such as inserting and retrieving the instance in a hash-based collection.
     *
     * @return The hash code of the 'id' property of the current instance.
     */
    override fun hashCode(): Int {
        return this.id.hashCode()
    }
}
    /**
     * This function is used to calculate the position vector of a Place.
     * The function takes in an azimuth and a LatLng object as parameters.
     * It first retrieves the LatLng of the Place and calculates the heading from the passed LatLng to the Place's LatLng.
     * Then it calculates the x, y, and z coordinates of the position vector using the azimuth, heading, and a fixed radius.
     * The function returns the calculated position vector.
     *
     * @param azimuth The azimuth to use in the calculation.
     * @param latLng The LatLng to calculate the heading from.
     * @return The calculated position vector.
     */
    fun Place.getPositionVector(azimuth: Float, latLng: LatLng): Vector3 {
        val placeLatLng = this.geometry.location.latLng
        val heading = latLng.sphericalHeading(placeLatLng)
        val r = -2f
        val x = r * sin(azimuth + heading).toFloat()
        val y = 1f
        val z = r * cos(azimuth + heading).toFloat()
        return Vector3(x, y, z)
    }

    /**
     * A data class representing the geometry of a Place.
     * It contains a single property 'location' of type GeometryLocation.
     */
    data class Geometry(
        val location: GeometryLocation
    )

    /**
     * A data class representing the location of a Place's geometry.
     * It contains two properties 'lat' and 'lng' representing the latitude and longitude of the location respectively.
     * It also contains a getter 'latLng' that returns a LatLng object constructed from the 'lat' and 'lng' properties.
     */
    data class GeometryLocation(
        val lat: Double,
        val lng: Double
    ) {
        val latLng: LatLng
            get() = LatLng(lat, lng)
    }

