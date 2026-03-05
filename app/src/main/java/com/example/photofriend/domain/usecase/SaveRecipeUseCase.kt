package com.example.photofriend.domain.usecase

import com.example.photofriend.domain.model.FilmSimulationRecipe
import com.example.photofriend.domain.repository.CameraRepository
import javax.inject.Inject

class SaveRecipeUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    suspend operator fun invoke(recipe: FilmSimulationRecipe) = repository.saveRecipe(recipe)
}
