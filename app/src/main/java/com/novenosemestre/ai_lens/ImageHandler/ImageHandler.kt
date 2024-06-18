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
import com.novenosemestre.ai_lens.ImageSearchHandler.ImageSearchHandler
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

    private lateinit var cameraImageUri: Uri                                                      // Uri of the image taken with the camera
    private val client = OkHttpClient()                                                           // HTTP client to make requests
    private val apiKey = ""//"AIzaSyC6SGZCC8KvjHiOimKoKrJXXuJ4XmW1Mn8"                                // Google Cloud API Key
    private val cx  = ""//"86c439b6f8b854a06"                                                         // Google Custom Search API Key
    private val imageSearch =  ImageSearchHandler(cx, apiKey, activity.applicationContext)


    /**
     * This is an instance of ActivityResultLauncher for handling the result from an activity that is started with
     * the intention of picking an image from the gallery. The ActivityResultContracts.StartActivityForResult() contract
     * is used to launch the activity.
     *
     * When the result is received, the resultCode is checked. If the result is Activity.RESULT_OK, it means that the
     * operation was successful. The URI of the selected image is then retrieved from the Intent data.
     *
     * If the image URI is not null, the startCrop function is called with the image URI as the argument. The startCrop
     * function is expected to handle the cropping of the selected image.
     */
     private val galleryLauncher: ActivityResultLauncher<Intent> = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
     ) { result ->
         if (result.resultCode == Activity.RESULT_OK) {
             val imageUri: Uri? = result.data?.data
             imageUri?.let { startCrop(it) }
         }
     }

    /**
     * This is an instance of ActivityResultLauncher for handling the result from an activity that is started with
     * the intention of capturing an image from the camera. The ActivityResultContracts.StartActivityForResult() contract
     * is used to launch the activity.
     *
     * When the result is received, the resultCode is checked. If the result is Activity.RESULT_OK, it means that the
     * operation was successful. The URI of the captured image is then retrieved from the cameraImageUri variable.
     *
     * If the cameraImageUri is initialized, the startCrop function is called with the cameraImageUri as the argument.
     * The startCrop function is expected to handle the cropping of the captured image.
     */
    private val cameraLauncher: ActivityResultLauncher<Intent> = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (::cameraImageUri.isInitialized) {
                startCrop(cameraImageUri)
            } else {
                println("Error: cameraImageUri no está inicializado")
            }
        }
    }

    /**
     * This is an instance of ActivityResultLauncher for handling the result from an activity that is started with
     * the intention of cropping an image. The ActivityResultContracts.StartActivityForResult() contract
     * is used to launch the activity.
     *
     * When the result is received, the resultCode is checked. If the result is Activity.RESULT_OK, it means that the
     * operation was successful. The URI of the cropped image is then retrieved from the result data.
     *
     * If the result URI is not null, the image view is updated with the cropped image and the analyzeImage function
     * is called with the file path of the cropped image as the argument. The analyzeImage function is expected to
     * handle the analysis of the cropped image.
     */
    private val cropLauncher: ActivityResultLauncher<Intent> = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                imageView.setImageURI(it)
                analyzeImage(File(it.path))
            }
        }
    }

    /**
     * This function is used to open the device's gallery for image selection.
     * It creates an Intent with the ACTION_PICK action and the EXTERNAL_CONTENT_URI of the MediaStore.Images.Media.
     * This intent is then launched with the galleryLauncher, which is an instance of ActivityResultLauncher.
     * The galleryLauncher is expected to handle the result of the activity.
     */
    fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    /**
     * This function is used to capture an image from the device's camera.
     * It creates an Intent with the ACTION_IMAGE_CAPTURE action and checks if there is an activity available to handle the intent.
     *
     * If there is an activity available, it creates a temporary file for the image to be captured using the createImageFile function.
     * If the file is successfully created, it gets a URI for the file using the FileProvider's getUriForFile method and sets it as the cameraImageUri.
     * The URI is then added to the intent as an extra with the EXTRA_OUTPUT key.
     *
     * The intent is then launched with the cameraLauncher, which is an instance of ActivityResultLauncher.
     * The cameraLauncher is expected to handle the result of the activity.
     *
     * If there is an exception while creating the file, the exception is printed and the function returns without launching the intent.
     */
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

    /**
     * This function is used to create a temporary file for storing an image.
     * It gets the directory for storing pictures from the external storage using the getExternalFilesDir method of the activity.
     * The directory is then used as the directory in which the file is created.
     *
     * The file is created as a temporary file with a unique name using the createTempFile method of the File class.
     * The prefix of the file is "JPEG_" followed by the current time in milliseconds, and the suffix is ".jpg".
     *
     * The function returns the created file.
     */
    private fun createImageFile(): File {
        val storageDir: File? = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${System.currentTimeMillis()}_",
            ".jpg",
            storageDir
        )
    }

    /**
     * This function is used to start the cropping of an image.
     * It takes a URI as an argument, which is the URI of the image to be cropped.
     *
     * The function creates a destination URI for the cropped image. The destination URI is a URI of a file in the cache directory of the activity.
     * The file is named "cropped_" followed by the current time in milliseconds and the extension ".jpg".
     *
     * The function then creates an instance of UCrop.Options and enables the free style crop option.
     *
     * A crop intent is created using the UCrop.of method with the source URI and the destination URI as arguments.
     * The options are added to the UCrop instance using the withOptions method.
     * The intent is then retrieved from the UCrop instance using the getIntent method.
     *
     * The crop intent is then launched with the cropLauncher, which is an instance of ActivityResultLauncher.
     * The cropLauncher is expected to handle the result of the activity.
     */
    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(File(activity.cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        val options = UCrop.Options()
        options.setFreeStyleCropEnabled(true)
        val cropIntent = UCrop.of(uri, destinationUri)
            .withOptions(options)
            .getIntent(activity)
        cropLauncher.launch(cropIntent)
    }

    /**
     * This function is used to analyze an image and identify objects within it.
     * It takes a File as an argument, which is the image to be analyzed.
     *
     * The function reads the bytes of the image file and encodes them into a Base64 string.
     * It then creates a JSON request body with the Base64 string of the image and the type of analysis to be performed.
     *
     * The function makes a POST request to the Google Cloud Vision API with the JSON request body.
     * The API key for the Google Cloud Vision API is included in the URL of the request.
     *
     * The function handles the response from the API in the onResponse method of the Callback.
     * If the response contains localizedObjectAnnotations, it iterates over them and finds the object with the highest score that is greater than or equal to 0.5.
     * If such an object is found, it updates the text view with the name of the object and searches for similar images using the ImageSearchHandler.
     *
     * If the request fails, the function prints the error message in the onFailure method of the Callback.
     */
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
                    //println("Response: $jsonString")

                    val jsonObject = JSONObject(jsonString)

                    val responses = jsonObject.getJSONArray("responses")
                    val firstResponse = responses.getJSONObject(0)

                    if (firstResponse.has("localizedObjectAnnotations")) {
                        val localizedObjectAnnotations = firstResponse.getJSONArray("localizedObjectAnnotations")

                        var highestScore = 0.0 // Highest score found for an object
                        var highestScoreName: String? = null // Name of the object with the highest score

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