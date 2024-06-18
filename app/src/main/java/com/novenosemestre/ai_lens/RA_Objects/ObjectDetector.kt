package com.novenosemestre.ai_lens.RA_Objects

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions

class ObjectDetector(private val image: Bitmap, private val idAnalyxer: IdAnalyzer) {

    /**
     * A lazy property that builds a LocalModel.
     * The LocalModel is built with an asset file path set to "ssd_mobilenet_v1_1_metadata_1.tflite".
     * This property is initialized the first time it is accessed.
     *
     * @return The built LocalModel.
     */
    private val localModel by lazy {
        LocalModel.Builder()
            .setAssetFilePath("ssd_mobilenet_v1_1_metadata_1.tflite")
            // or .setAbsoluteFilePath("absolute_file_path_to_tflite_model")
            .build()
    }

    /**
     * A lazy property that builds CustomObjectDetectorOptions.
     * The CustomObjectDetectorOptions are built with the localModel, detector mode set to STREAM_MODE, classification enabled,
     * classification confidence threshold set to 0.5, and max per object label count set to 3.
     * This property is initialized the first time it is accessed.
     *
     * @return The built CustomObjectDetectorOptions.
     */
    private val options by lazy {
        CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .setClassificationConfidenceThreshold(0.5f)
            .setMaxPerObjectLabelCount(3)
            .build()
    }

    // [END create_custom_options]
   /**
    * A lazy property that builds an ObjectDetector.
    * The ObjectDetector is built with the options property.
    * This property is initialized the first time it is accessed.
    *
    * @return The built ObjectDetector.
    */
   private val objectDetector by lazy { ObjectDetection.getClient(options) }

    /**
     * A variable to keep track of the count. Initially set to 0.
     */
    private var count = 0

    /**
     * Function to use the custom object detector.
     * This function first checks if the isc is not running. If it is not, it processes an InputImage created from the image property with rotation set to 0.
     * It adds a success listener, a failure listener, and a complete listener to the process.
     * The success listener sets iscRunning to true, logs the size of the results, and calls the idAnalyxer function for each result with labels.
     * The failure listener sets iscRunning to false and prints the stack trace of the exception.
     * The complete listener sets iscRunning to false.
     * If an exception is thrown during the process, the function catches it and prints its stack trace.
     */
    fun useCustomObjectDetector() {

        try {

            if (!Constants.iscRunning) {
                objectDetector.process(InputImage.fromBitmap(image, 0))
                    .addOnSuccessListener { results ->

                        Constants.iscRunning = true
                        Log.e("labels", "${results.size}")
                        results?.forEach {
                            if (it.labels.size > 0) {
                                idAnalyxer(it)
                            }

                        }

                    }
                    .addOnFailureListener { e ->

                        Constants.iscRunning = false
                        e.printStackTrace()

                    }.addOnCompleteListener {
                        Constants.iscRunning = false
                    }
            }
        }   catch (e: Exception) {
            e.printStackTrace()
        }

    }
    // [END process_image]


    // [END read_results_custom]
}

