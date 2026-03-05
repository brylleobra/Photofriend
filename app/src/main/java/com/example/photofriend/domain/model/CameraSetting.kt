package com.example.photofriend.domain.model

data class CameraSetting(
    val id: String,
    val cameraId: String,
    val category: SettingCategory,
    val name: String,
    val description: String,
    val options: List<String>,
    val defaultValue: String
)

enum class SettingCategory {
    COLOR, EXPOSURE, FOCUS, DRIVE, OUTPUT
}
