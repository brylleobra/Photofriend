package com.example.photofriend.domain.model

data class CameraModel(
    val id: String,
    val brand: String,
    val name: String,
    val sensorSize: String,
    val megapixels: Int,
    val filmSimulationCount: Int,
    val releaseYear: Int
)
