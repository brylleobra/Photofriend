package com.example.photofriend.di

import com.example.photofriend.domain.model.FilmSimulationRecipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectedRecipeStore @Inject constructor() {

    private val _recipe = MutableStateFlow<FilmSimulationRecipe?>(null)
    val recipe: StateFlow<FilmSimulationRecipe?> = _recipe.asStateFlow()

    fun select(recipe: FilmSimulationRecipe) {
        _recipe.value = recipe
    }

    fun clear() {
        _recipe.value = null
    }
}
