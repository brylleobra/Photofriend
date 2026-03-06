package com.example.photofriend.data.repository

import com.example.photofriend.data.local.db.dao.CameraModelDao
import com.example.photofriend.data.local.db.dao.CameraSettingDao
import com.example.photofriend.data.local.db.dao.RecipeDao
import com.example.photofriend.data.local.seed.CameraSeeds
import com.example.photofriend.data.local.seed.RecipeSeeds
import com.example.photofriend.data.mapper.toDomain
import com.example.photofriend.data.mapper.toEntity
import com.example.photofriend.domain.model.CameraModel
import com.example.photofriend.domain.model.CameraSetting
import com.example.photofriend.domain.model.FilmSimulationRecipe
import com.example.photofriend.domain.repository.CameraRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraRepositoryImpl @Inject constructor(
    private val cameraModelDao: CameraModelDao,
    private val cameraSettingDao: CameraSettingDao,
    private val recipeDao: RecipeDao
) : CameraRepository {

    override fun getCameraModels(): Flow<List<CameraModel>> =
        cameraModelDao.getAll()
            .onStart { seedCamerasIfEmpty() }
            .map { entities -> entities.map { it.toDomain() } }

    override fun getCameraSettings(cameraId: String): Flow<List<CameraSetting>> =
        cameraSettingDao.getByCameraId(cameraId)
            .onStart { seedSettingsIfEmpty(cameraId) }
            .map { entities -> entities.map { it.toDomain() } }

    override fun getBuiltInRecipes(): Flow<List<FilmSimulationRecipe>> =
        recipeDao.getBuiltIn()
            .onStart { seedRecipesIfEmpty() }
            .map { entities -> entities.map { it.toDomain() } }

    override fun getSavedRecipes(): Flow<List<FilmSimulationRecipe>> =
        recipeDao.getSaved()
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveRecipe(recipe: FilmSimulationRecipe) {
        recipeDao.insert(recipe.toEntity())
    }

    override suspend fun deleteRecipe(recipeId: String) {
        recipeDao.delete(recipeId)
    }

    private suspend fun seedCamerasIfEmpty() {
        cameraModelDao.insertAll(CameraSeeds.cameras)
    }

    private suspend fun seedSettingsIfEmpty(cameraId: String) {
        // IGNORE conflict strategy means existing rows are skipped — safe to call every time.
        // This ensures newly added settings (e.g. Aperture) appear even on existing installs.
        if (cameraId == "fujifilm_xt30iii") {
            cameraSettingDao.insertAll(CameraSeeds.xt30iiiSettings)
        }
    }

    private suspend fun seedRecipesIfEmpty() {
        if (recipeDao.countBuiltIn() == 0) {
            RecipeSeeds.builtInRecipes.forEach { recipeDao.insert(it) }
        }
    }
}
