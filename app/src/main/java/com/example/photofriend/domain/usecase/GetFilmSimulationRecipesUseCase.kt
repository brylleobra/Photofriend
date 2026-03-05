package com.example.photofriend.domain.usecase

import com.example.photofriend.domain.model.FilmSimulationRecipe
import com.example.photofriend.domain.repository.CameraRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFilmSimulationRecipesUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    fun builtIn(): Flow<List<FilmSimulationRecipe>> = repository.getBuiltInRecipes()
    fun saved(): Flow<List<FilmSimulationRecipe>> = repository.getSavedRecipes()
}
