plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.0"
}

android {
    namespace = "com.novenosemestre.ai_lens"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.novenosemestre.ai_lens"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
packagingOptions {
    exclude("META-INF/INDEX.LIST")
    exclude("META-INF/DEPENDENCIES")
}
    buildFeatures {
        mlModelBinding = true
        viewBinding = true
        dataBinding = true
    }
    configurations.all {
        exclude(group = "com.google.flatbuffers", module = "flatbuffers-java")
    }
}

//apply(from = "download_models.gradle")



dependencies {
    //implementation("com.android.support:multidex:1.0.3")
    implementation("com.google.ar:core:1.43.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
   /* implementation("org.tensorflow:tensorflow-lite-support:0.1.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.3.0")*/
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.3.12")
    implementation("com.github.yalantis:ucrop:2.2.8")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.google.code.gson:gson:2.10.1")

    // Cloud Vision API dependencies
    implementation("com.google.cloud:google-cloud-vision:3.41.0")
    implementation("com.google.protobuf:protobuf-java:3.25.3")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.google.cloud:libraries-bom:19.2.1")
    implementation("com.google.api.grpc:proto-google-cloud-vision-v1:3.41.0")
    implementation("com.google.api.grpc:proto-google-common-protos:2.39.0")
    implementation("com.google.api-client:google-api-client-android:1.33.0")
    implementation("com.google.http-client:google-http-client-android:1.40.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.31.5")

    // Auth and credentials dependencies
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")
    implementation("com.google.auth:google-auth-library-credentials:1.23.0")

    // Volley dependencies
    implementation("com.android.volley:volley:1.2.1")

    implementation("com.github.bumptech.glide:glide:4.12.0")

    //cardview
    implementation("androidx.cardview:cardview:1.0.0")

    //searchResult
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("com.android.volley:volley:1.2.1")

    //TensorFlow Lite
   /* implementation("org.tensorflow:tensorflow-lite:2.6.0")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.1.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.1.0")
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.1.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.8.0")*/

   // implementation("org.tensorflow:tensorflow-lite-gpu:2.10.0")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.2")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.2")
    implementation("org.tensorflow:tensorflow-lite:2.10.0")

    //Augmented Reality
    //implementation("com.google.ar.sceneform.ux:sceneform-ux:1.17.1")
   // implementation("com.google.ar.sceneform:core:1.17.1")

    implementation("com.google.mlkit:object-detection:17.0.1")
    implementation("com.google.mlkit:object-detection-custom:17.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("com.gorisse.thomas.sceneform:sceneform:1.19.3")
    implementation("com.google.mlkit:object-detection:17.0.1")
    implementation("com.google.mlkit:object-detection-custom:17.0.1")

    implementation("de.javagl:obj:0.2.1")
    implementation ("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation ("androidx.lifecycle:lifecycle-common-java8:2.8.1")
    //implementation("io.grpc:grpc-okhttp:1.42.0")

    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation("com.google.cloud:libraries-bom:19.2.1")


    /*implementation ("androidx.camera:camera-core:1.3.3")
    implementation ("androidx.camera:camera-camera2:1.3.3")
    implementation ("androidx.camera:camera-lifecycle:1.3.3")
    implementation ("androidx.camera:camera-view:1.3.3")
    implementation ("androidx.camera:camera-extensions:1.3.3")*/

    // You need to build grpc-java to obtain these libraries below.
    implementation("io.grpc:grpc-protobuf:1.64.0")
    /*implementation("io.grpc:grpc-stub:1.42.0")
    implementation("io.grpc:grpc-testing:1.42.0")
    implementation("io.grpc:grpc-api:1.42.0")*/
    implementation("io.grpc:grpc-okhttp:1.64.0")
    implementation("io.grpc:grpc-stub:1.64.0")

    //google maps
    implementation("com.google.android.gms:play-services-location:17.0.0")
    implementation("com.google.android.gms:play-services-maps:17.0.0")
    implementation("com.google.maps.android:maps-utils-ktx:0.2")
    implementation("com.google.android.gms:play-services-places:17.0.0")
    implementation("com.google.maps.android:maps-utils-ktx:0.2")


    //retrofit
    implementation("com.squareup.retrofit2:retrofit:2.7.1")
    implementation("com.squareup.retrofit2:converter-gson:2.7.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

}