# Artificial Intelligence Application - AI Lens

This application leverages artificial intelligence to enhance the user experience with advanced functionalities. Below, we detail the main features and technologies utilized.

## General description

AI Lens is a mobile application designed to:

- **Detect objects in images**: Use machine learning models to identify and classify objects.
- **Perform product searches**: Based on object detection, the application allows you to search for similar products online.
- **Augmented Reality (AR)**: Shows the name of the detected objects in the real-time view.
- **Google Maps**: Integrate maps to show the user's location and provide a contextualized AR experience.

 <img src="images/ai-lens.png" alt="ai-lens" width="200"/>

## Main Features

### 2.1 Object Detection in Images

- **Image upload**: Allows the user to upload an image from the gallery for object detection.
- **Photo capture**: Allows the user to take a photo using the camera to detect objects in real time.
  
<div style="display: flex;">
 <img src="images/galery.png" alt="ai-lens" width="200"/>
 <img src="images/galery2.png" alt="ai-lens" width="200"/>
</div>

<div style="display: flex;">
 <img src="images/picture.png" alt="ai-lens" width="200"/>
 <img src="images/picture2.png" alt="ai-lens" width="200"/>
</div>
  
### 2.2 Product Search

- **Object-based results**: Uses object detection to suggest products similar to those identified.
- **Integration with online search services**: Make it easy to find relevant products using search engines.
  
   <div style="display: flex;">
     <img src="images/galery3.png" alt="ai-lens" width="200"/>
     <img src="images/picture3.png" alt="ai-lens" width="200"/>
   </div>
   
### 2.3 Augmented Reality (AR)

- **Real time view**: Shows the names of detected objects overlaid on the camera view.
- **Intuitive interaction**: Allows the user to explore detected objects with augmented reality.

   <div style="display: flex;">
     <img src="images/ra.png" alt="ai-lens" width="200"/>
     <img src="images/ra2.png" alt="ai-lens" width="200"/>
   </div>
   
### 2.4 Google Maps

- **Location display**: Shows the user's location on a map.
- **Contextual AR**: Overlay location in augmented reality to improve navigation and interaction.

<img src="images/maps.png" alt="ai-lens" width="200"/>

## Used technology

- **TensorFlow Lite**: For the object detection model and inference on mobile devices.   
- **Google AI Vertex**: For training machine learning models.              [Google Cloud](https://console.cloud.google.com/vertex-ai?referrer=search&project=marine-balm-424004-g6)
- **Google Maps**: For the integration of maps and AR functionality.
- **Android**: Mobile platform for the implementation of the application.
- **Programmable Search Engine**: A customizable search engine that allows users to create and control their own web search experience.   [Search Engine](https://programmablesearchengine.google.com/)

## Installation

1. Clone this repository.
2. Install the necessary dependencies using `gradle`.
3. Configure the necessary Google APIs for Maps and AI Vertex.

    - ***Cloud Vision API***
    - ***Custom Search API***
    - ***Vertex AI API***
    - ***Maps SDK for Android***
    - ***Places API***

Location where the credential.json file should be placed

```javascript
 path : res/raw/credential.json

Credential structure example:

{
  "type": "service_account",
  "project_id": "xxxxxxxxxxxx",
  "private_key_id": "xxxxxxxxxxxxxx",
  "private_key": "-----BEGIN PRIVATE KEY-----\xxxxx\n-----END PRIVATE KEY-----\n",
  "client_email": "xxxxxxxxxx.iam.gserviceaccount.com",
  "client_id": "xxxxxxxxxxxxxxx",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.iam.gserviceaccount.com",
  "universe_domain": "googleapis.com"
}

```


## Contribute ⚡

Contributions are welcome. To contribute, follow the steps to clone the repository and make a pull request. 🚀
