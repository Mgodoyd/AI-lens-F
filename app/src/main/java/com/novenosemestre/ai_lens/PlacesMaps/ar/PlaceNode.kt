package com.novenosemestre.ai_lens.PlacesMaps.ar


import android.content.Context
import android.view.View
import android.widget.TextView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.ViewRenderable
import com.novenosemestre.ai_lens.PlacesMaps.model.Place
import com.novenosemestre.ai_lens.R

class PlaceNode(
    val context: Context,
    val place: Place?
) : Node() {

    private var placeRenderable: ViewRenderable? = null // Renderable for the place
    private var textViewPlace: TextView? = null

    /**
     * This function is part of the Node lifecycle in ARCore and is called when the Node is activated.
     * Activation occurs when the Node is added to the scene graph and the scene is not null.
     *
     * The function first checks if the scene is null. If it is, the function returns immediately.
     * Then it checks if the 'placeRenderable' is not null. If it is not null, the function returns immediately.
     * This is to prevent re-initializing the 'placeRenderable' if it has already been initialized.
     *
     * Then it creates a ViewRenderable using the builder pattern.
     * The ViewRenderable is built with the context and a layout resource (R.layout.palce_view).
     * The built ViewRenderable is then set as the renderable of this Node and stored in the 'placeRenderable' property.
     *
     * If the 'place' property is not null, it finds the TextView in the renderable's view with the id 'placeName' and stores it in the 'textViewPlace' property.
     * It then sets the text of the TextView to the name of the place and makes the TextView visible.
     */
    override fun onActivate() {
        super.onActivate()

        if (scene == null) {
            return
        }

        if (placeRenderable != null) {
            return
        }

        ViewRenderable.builder()
            .setView(context, R.layout.palce_view)
            .build()
            .thenAccept { renderable ->
                setRenderable(renderable)
                placeRenderable = renderable

              place?.let {
                textViewPlace = renderable.view.findViewById(R.id.placeName)
                println("Nombre del local " + it.name)
                textViewPlace?.apply {
                    text = it.name
                    visibility = View.VISIBLE
                }
            }
        }
    }

   /**
    * This function is used to toggle the visibility of the information window for the place represented by this node.
    * The information window is a TextView that displays the name of the place.
    *
    * The function first checks if 'textViewPlace' is not null. If it is null, the function returns immediately.
    * Then it toggles the visibility of 'textViewPlace'. If it is currently visible, it is made invisible, and vice versa.
    *
    * The function then gets the parent of this node and retrieves its children.
    * It filters the children to get only those that are instances of PlaceNode and are not this node.
    * For each of these nodes, it sets the visibility of their 'textViewPlace' to GONE, effectively hiding their information windows.
    */
   fun showInfoWindow() {
       // Show text
       textViewPlace?.let {
           it.visibility = if (it.visibility == View.VISIBLE) View.GONE else View.VISIBLE
       }

       // Hide text for other nodes
       this.parent?.children?.filter {
           it is PlaceNode && it != this
       }?.forEach {
           (it as PlaceNode).textViewPlace?.visibility = View.GONE
       }
   }
}