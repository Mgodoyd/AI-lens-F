package com.novenosemestre.ai_lens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ar.core.ArCoreApk
import com.novenosemestre.ai_lens.ImageHandler.ImageHandler
import com.novenosemestre.ai_lens.ImageSearchHandler.ImageSearchHandler
import com.novenosemestre.ai_lens.ImageSearchHandler.ResultAdapter
import com.novenosemestre.ai_lens.PlacesMaps.PlacesMapsActivity2
import com.novenosemestre.ai_lens.RA_Objects2.MainActivity2

class MainActivity : AppCompatActivity() {

    private lateinit var imageHandler: ImageHandler
    private lateinit var takePictureButton: Button
    private lateinit var resultAdapter: ResultAdapter
    private lateinit var recyclerView: RecyclerView


    /**
     * Called when the activity is being created.
     * This function sets the content view, initializes the ImageHandler with the ImageView,
     * sets the click listeners for the open gallery and take picture buttons,
     * configures the RecyclerView and its adapter, sets the click listeners for the AR and maps buttons,
     * and checks if the device supports ARCore.
     *
     * @param savedInstanceState The saved instance state bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the ImageHandler with the ImageView
        val imageView: ImageView = findViewById(R.id.imageView)
        imageHandler = ImageHandler(this, imageView)

        // Set the click listeners for the open gallery and take picture buttons
        val openGalleryButton: Button = findViewById(R.id.button_open_gallery)
        takePictureButton = findViewById(R.id.button_take_picture)
        openGalleryButton.setOnClickListener { imageHandler.openGallery() }
        takePictureButton.setOnClickListener { checkPermission() }

        // Configure the RecyclerView and its adapter
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        resultAdapter = ResultAdapter(mutableListOf())
        recyclerView.adapter = resultAdapter

        /* val buttonRA = findViewById<Button>(R.id.button_ra)
        buttonRA.setOnClickListener {
            val intent = Intent(this, TextureViewActivity::class.java)
            startActivity(intent)
        }*/

        /* val buttonRA = findViewById<Button>(R.id.button_ra)
         buttonRA.setOnClickListener {
             val intent = Intent(this, ArDisplayFragmentActivity::class.java)
             startActivity(intent)
         }*/

        // Set the click listeners for the AR and maps buttons
        val buttonRA = findViewById<Button>(R.id.button_ra)
        buttonRA.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }
        val buttonMaps = findViewById<Button>(R.id.button_maps)
        buttonMaps.setOnClickListener {
            val intent = Intent(this, PlacesMapsActivity2::class.java)
            startActivity(intent)
        }

        // Check if the device supports ARCore
        maybeEnableArButton()
    }

    /**
     * Checks if the camera permission is granted.
     * If the permission is not granted, it requests the permission.
     * If the permission is granted, it opens the camera.
     */
    fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted
            requestCameraPermission()
        } else {
            // Open camera
            imageHandler.dispatchTakePictureIntent()
        }
    }

    /**
     * Checks if the device supports ARCore.
     * If the device supports ARCore, it makes the AR button visible and enables it.
     * If the device does not support ARCore, it makes the AR button invisible and disables it.
     */
    fun maybeEnableArButton() {
        ArCoreApk.getInstance().checkAvailabilityAsync(this) { availability ->
            val buttonRA2 = findViewById<Button>(R.id.button_ra)
            if (availability.isSupported) {
                buttonRA2.visibility = View.VISIBLE
                buttonRA2.isEnabled = true
                println("El dispositivo es compatible con ARCore")
            } else {
                buttonRA2.visibility = View.INVISIBLE
                buttonRA2.isEnabled = false
                println("El dispositivo no es compatible con ARCore")
            }
        }
    }

    /**
     * Requests the camera permission.
     * If the user has denied the permission and chosen not to be asked again, it shows a toast message.
     * Otherwise, it requests the permission.
     */
    fun requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            // The user has denied the permission and chosen not to be asked again
            Toast.makeText(this, "Permisos rechazados", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 777)
        }
    }

    /**
     * Handles the result of the permission request.
     * If the camera permission is granted, it opens the camera.
     * If the camera permission is not granted, it shows a toast message.
     *
     * @param requestCode The request code passed in requestPermissions().
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 777) { // Our permissions
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // The permission has been granted
                imageHandler.dispatchTakePictureIntent()
            } else { // The permission has not been granted
                Toast.makeText(this, "Permiso rechazado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Updates the results of the adapter.
     * This function is run on the user interface thread.
     *
     * @param results The search results to update.
     */
    fun updateSearchResults(results: List<ImageSearchHandler.SearchResult>) {
        runOnUiThread {
            resultAdapter.updateResults(results)
        }
    }
}
