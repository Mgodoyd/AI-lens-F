package com.novenosemestre.ai_lens.ImageHandler

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.google.cloud.vision.v1.*
import com.google.gson.Gson
import com.novenosemestre.ai_lens.ImageSearchHandler.ImageSearchHandler
import com.novenosemestre.ai_lens.ImageSearchHandler.ResultAdapter
import com.novenosemestre.ai_lens.MainActivity
import com.novenosemestre.ai_lens.R
import com.yalantis.ucrop.UCrop
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Base64


class ImageHandler(private val activity: MainActivity, private val imageView: ImageView) {

    private lateinit var cameraImageUri: Uri
    private val gson = Gson()
    private val client = OkHttpClient()
    private val apiKey = ""
    private val cx  = ""
    private val imageSearch =  ImageSearchHandler(cx, apiKey, activity.applicationContext)
    private val resultAdapter: ResultAdapter = ResultAdapter(mutableListOf())




    private val galleryLauncher: ActivityResultLauncher<Intent> = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            imageUri?.let { startCrop(it) }
        }
    }

    private val cameraLauncher: ActivityResultLauncher<Intent> = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

            println("result cameraImageUri $result")

            if (::cameraImageUri.isInitialized) {
                startCrop(cameraImageUri)
            } else {
                println("Error: cameraImageUri no está inicializado")
            }
        }
    }

    private val cropLauncher: ActivityResultLauncher<Intent> = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {  imageView.setImageURI(it)
                analyzeImage(File(it.path)) }
        }
    }

    fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun dispatchTakePictureIntent() {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(activity.packageManager) != null) {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    null
                }
                photoFile?.also {
                    cameraImageUri = FileProvider.getUriForFile(
                        activity,
                        "com.novenosemestre.ai_lens.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
                    cameraLauncher.launch(takePictureIntent)
                }
            }
    }

    private fun createImageFile(): File {
        val storageDir: File? = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${System.currentTimeMillis()}_",
            ".jpg",
            storageDir
        )
    }

    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(File(activity.cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        val options = UCrop.Options()
        options.setFreeStyleCropEnabled(true)
        val cropIntent = UCrop.of(uri, destinationUri)
            .withOptions(options)
            .getIntent(activity)
        cropLauncher.launch(cropIntent)
    }

    private fun analyzeImage(imageFile: File) {
        val imageBytes = imageFile.inputStream().readBytes()
        val base64Image = Base64.getEncoder().encodeToString(imageBytes)

        val url = "https://vision.googleapis.com/v1/images:annotate?key=$apiKey"

        val jsonRequest = """
            {
              "requests": [
                {
                  "image": {
                    "content": "$base64Image"
                  },
                  "features": [
                    {
                      "type": "OBJECT_LOCALIZATION"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), jsonRequest))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Error al realizar la solicitud: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                responseBody?.let { jsonString ->
                    println("Respuesta: $jsonString")

                    val jsonObject = JSONObject(jsonString)

                    val responses = jsonObject.getJSONArray("responses")
                    val firstResponse = responses.getJSONObject(0)

                    if (firstResponse.has("localizedObjectAnnotations")) {
                        val localizedObjectAnnotations = firstResponse.getJSONArray("localizedObjectAnnotations")

                        var highestScore = 0.0 // Puntaje más alto encontrado
                        var highestScoreName: String? = null // Nombre del objeto con el puntaje más alto

                        for (i in 0 until localizedObjectAnnotations.length()) {
                            val annotation = localizedObjectAnnotations.optJSONObject(i)
                            val name = annotation?.optString("name")
                            val score = annotation?.optDouble("score")
                            if ((score?.compareTo(0.5) ?: -1) >= 0 && (score ?: 0.0) > highestScore) {
                                highestScore = score ?: 0.0
                                highestScoreName = name
                            }
                        }

                        if (highestScoreName != null) {
                            println("Object Name: $highestScoreName")
                            activity.findViewById<TextView>(R.id.textViewTitle).text = highestScoreName

                            imageSearch.searchSimilarImages(highestScoreName) { results ->
                                println("Searching for similar images... Results: $results")
                                if (results.isNotEmpty()) {
                                    activity.updateSearchResults(results)
                                } else {
                                    println("No results found.")
                                }
                            }
                        } else {
                            println("No object found with score >= 0.5.")
                        }
                    } else {
                        println("No localizedObjectAnnotations found.")
                    }
                }
            }
        })
    }
}