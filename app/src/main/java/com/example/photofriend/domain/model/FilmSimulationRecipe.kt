package com.example.photofriend.domain.model

data class FilmSimulationRecipe(
    val id: String,
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
    val clarity: Int = 0,
    val isoMin: Int,
    val isoMax: Int,
    val tags: List<String>,
    val isUserSaved: Boolean = false
)
