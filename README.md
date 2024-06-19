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
- 
<div style="display: flex;">
 <img src="images/galery.png" alt="ai-lens" width="200"/>
 <img src="images/galery2.png" alt="ai-lens" width="200"/>
 <img src="images/galery3.png" alt="ai-lens" width="200"/>
</div>

<div style="display: flex;">
 <img src="images/picture.png" alt="ai-lens" width="200"/>
 <img src="images/picture2.png" alt="ai-lens" width="200"/>
 <img src="images/picture3.png" alt="ai-lens" width="200"/>
</div>
  
### 2.2 Búsqueda de Productos

- **Resultados basados en objetos**: Utiliza la detección de objetos para sugerir productos similares a los identificados.
- **Integración con servicios de búsqueda en línea**: Facilita la búsqueda de productos relevantes utilizando motores de búsqueda.

### 2.3 Realidad Aumentada (AR)

- **Vista en tiempo real**: Muestra los nombres de los objetos detectados superpuestos en la vista de la cámara.
- **Interacción intuitiva**: Permite al usuario explorar los objetos detectados con realidad aumentada.

### 2.4 Google Maps

- **Visualización de ubicación**: Muestra la ubicación del usuario en un mapa.
- **AR contextual**: Superpone la ubicación en realidad aumentada para mejorar la navegación y la interacción.

## Tecnologías Utilizadas

- **TensorFlow Lite**: Para el modelo de detección de objetos y la inferencia en dispositivos móviles.
- **Google AI Vertex**: Para el entrenamiento de modelos de aprendizaje automático.
- **Google Maps API**: Para la integración de mapas y funcionalidad de AR.
- **Android**: Plataforma móvil para la implementación de la aplicación.

## Instalación

1. Clona este repositorio.
2. Instala las dependencias necesarias usando `gradle` o `maven`.
3. Configura las APIs de Google necesarias para Maps y AI Vertex.

## Contribuir

Las contribuciones son bienvenidas. Para contribuir, sigue los pasos para clonar el repositorio y realiza un pull request.

