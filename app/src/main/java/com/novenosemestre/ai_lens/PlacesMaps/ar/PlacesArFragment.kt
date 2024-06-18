package com.novenosemestre.ai_lens.PlacesMaps.ar

import android.Manifest
import com.google.ar.sceneform.ux.ArFragment

class PlacesArFragment : ArFragment() {

   /**
    * This function is part of the ArFragment lifecycle in ARCore and is used to request additional permissions.
    * In this case, it is requesting the ACCESS_FINE_LOCATION permission.
    * This permission is necessary for the app to access the device's GPS and network-based location.
    * The function returns an array of permissions to be requested.
    *
    * @return An array of permissions to be requested.
    */
    override fun getAdditionalPermissions(): Array<String> =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION).toTypedArray()
}
