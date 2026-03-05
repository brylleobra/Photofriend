package com.example.photofriend.domain.model

data class AISuggestion(
    val cameraId: String,
    val sceneDescription: String,
    val suggestedSettings: Map<String, String>,
    val filmSimulationRecipeName: String,
    val reasoning: String,
    val timestamp: Long = System.currentTimeMillis()
)
