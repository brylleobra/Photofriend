package com.example.photofriend.data.mapper

import com.example.photofriend.data.local.db.entity.CameraModelEntity
import com.example.photofriend.data.local.db.entity.CameraSettingEntity
import com.example.photofriend.data.local.db.entity.RecipeEntity
import com.example.photofriend.domain.model.CameraModel
import com.example.photofriend.domain.model.CameraSetting
import com.example.photofriend.domain.model.FilmSimulationRecipe
import com.example.photofriend.domain.model.SettingCategory

fun CameraModelEntity.toDomain() = CameraModel(
    id = id,
    brand = brand,
    name = name,
    sensorSize = sensorSize,
    megapixels = megapixels,
    filmSimulationCount = filmSimulationCount,
    releaseYear = releaseYear
)

fun CameraSettingEntity.toDomain() = CameraSetting(
    id = id,
    cameraId = cameraId,
    category = runCatching { SettingCategory.valueOf(category) }.getOrDefault(SettingCategory.OUTPUT),
    name = name,
    description = description,
    options = options.split("|"),
    defaultValue = defaultValue
)

fun RecipeEntity.toDomain() = FilmSimulationRecipe(
    id = id,
    name = name,
    description = description,
    filmSimulation = filmSimulation,
    grain = grain,
    colorChrome = colorChrome,
    colorChromeBlue = colorChromeBlue,
    whiteBalance = whiteBalance,
    wbShiftR = wbShiftR,
    wbShiftB = wbShiftB,
    highlights = highlights,
    shadows = shadows,
    color = color,
    sharpness = sharpness,
    noiseReduction = noiseReduction,
    clarity = clarity,
    isoMin = isoMin,
    isoMax = isoMax,
    tags = tags.split(",").filter { it.isNotBlank() },
    isUserSaved = isUserSaved
)

fun FilmSimulationRecipe.toEntity() = RecipeEntity(
    id = id,
    name = name,
    description = description,
    filmSimulation = filmSimulation,
    grain = grain,
    colorChrome = colorChrome,
    colorChromeBlue = colorChromeBlue,
    whiteBalance = whiteBalance,
    wbShiftR = wbShiftR,
    wbShiftB = wbShiftB,
    highlights = highlights,
    shadows = shadows,
    color = color,
    sharpness = sharpness,
    noiseReduction = noiseReduction,
    clarity = clarity,
    isoMin = isoMin,
    isoMax = isoMax,
    tags = tags.joinToString(","),
    isBuiltIn = false,
    isUserSaved = true
)
