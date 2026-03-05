package com.example.photofriend.ui.screen.recipedetail

import androidx.lifecycle.ViewModel
import com.example.photofriend.di.SelectedRecipeStore
import com.example.photofriend.domain.model.FilmSimulationRecipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    selectedRecipeStore: SelectedRecipeStore
) : ViewModel() {
    val recipe: StateFlow<FilmSimulationRecipe?> = selectedRecipeStore.recipe
}
