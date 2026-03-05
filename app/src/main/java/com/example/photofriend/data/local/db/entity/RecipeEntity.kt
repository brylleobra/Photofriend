package com.example.photofriend.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val filmSimulation: String,
    val grain: String,
    val colorChrome: String,
    val colorChromeBlue: String,
    val whiteBalance: String,
    val wbShiftR: Int,
    val wbShiftB: Int,
    val highlights: Int,
    val shadows: Int,
    val color: Int,
    val sharpness: Int,
    val noiseReduction: Int,
    val isoMin: Int,
    val isoMax: Int,
    val tags: String,         // comma-separated
    val isBuiltIn: Boolean,
    val isUserSaved: Boolean
)
