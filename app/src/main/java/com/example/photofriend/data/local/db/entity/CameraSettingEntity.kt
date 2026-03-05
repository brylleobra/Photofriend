package com.example.photofriend.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "camera_settings")
data class CameraSettingEntity(
    @PrimaryKey val id: String,
    val cameraId: String,
    val category: String,
    val name: String,
    val description: String,
    val options: String,      // JSON array stored as string
    val defaultValue: String
)
