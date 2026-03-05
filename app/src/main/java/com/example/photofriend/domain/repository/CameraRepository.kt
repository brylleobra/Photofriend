package com.example.photofriend.domain.repository

import com.example.photofriend.domain.model.CameraModel
import com.example.photofriend.domain.model.CameraSetting
import com.example.photofriend.domain.model.FilmSimulationRecipe
import kotlinx.coroutines.flow.Flow

interface CameraRepository {
    fun getCameraModels(): Flow<List<CameraModel>>
    fun getCameraSettings(cameraId: String): Flow<List<CameraSetting>>
    fun getBuiltInRecipes(): Flow<List<FilmSimulationRecipe>>
    fun getSavedRecipes(): Flow<List<FilmSimulationRecipe>>
    suspend fun saveRecipe(recipe: FilmSimulationRecipe)
    suspend fun deleteRecipe(recipeId: String)
}
