package com.example.photofriend.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "camera_models")
data class CameraModelEntity(
    @PrimaryKey val id: String,
    val brand: String,
    val name: String,
    val sensorSize: String,
    val megapixels: Int,
    val filmSimulationCount: Int,
    val releaseYear: Int
)
