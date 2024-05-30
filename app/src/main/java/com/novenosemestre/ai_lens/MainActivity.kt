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
import com.novenosemestre.ai_lens.RA_Objects2.MainActivity2

class MainActivity : AppCompatActivity() {

    private lateinit var imageHandler: ImageHandler
    private lateinit var takePictureButton: Button
    private lateinit var resultAdapter: ResultAdapter
    private lateinit var recyclerView: RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imageView: ImageView = findViewById(R.id.imageView)
        imageHandler = ImageHandler(this, imageView)


        val openGalleryButton: Button = findViewById(R.id.button_open_gallery)
        takePictureButton = findViewById(R.id.button_take_picture)

        openGalleryButton.setOnClickListener { imageHandler.openGallery() }
        takePictureButton.setOnClickListener { checkPermission() }

        // Configurar el RecyclerView y el adaptador
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

        val buttonRA = findViewById<Button>(R.id.button_ra)
        buttonRA.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }


        maybeEnableArButton()
    }
     fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Permiso no aceptado
            requestCameraPermission()
        } else {
            // Abrir cámara
            imageHandler.dispatchTakePictureIntent()
        }
    }

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
     fun requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            // El usuario rechazó los permisos
            Toast.makeText(this, "Permisos rechazados", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 777)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 777) { // Nuestros permisos
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // El permiso ha sido aceptado
                imageHandler.dispatchTakePictureIntent()
            } else { // El permiso no ha sido aceptado
                Toast.makeText(this, "Permiso rechazado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Agrega un método para actualizar los resultados del adaptador
    fun updateSearchResults(results: List<ImageSearchHandler.SearchResult>) {
        runOnUiThread {
            resultAdapter.updateResults(results)
        }
    }
}
