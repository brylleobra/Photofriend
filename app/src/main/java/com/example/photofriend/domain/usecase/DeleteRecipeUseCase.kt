package com.example.photofriend.domain.usecase

import com.example.photofriend.domain.repository.CameraRepository
import javax.inject.Inject

class DeleteRecipeUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    suspend operator fun invoke(recipeId: String) = repository.deleteRecipe(recipeId)
}
