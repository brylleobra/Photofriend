package com.example.photofriend.ui.screen.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photofriend.domain.model.FilmSimulationRecipe
import com.example.photofriend.domain.usecase.GetFilmSimulationRecipesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface RecipesUiState {
    object Loading : RecipesUiState
    data class Success(val recipes: List<FilmSimulationRecipe>) : RecipesUiState
}

@HiltViewModel
class RecipesViewModel @Inject constructor(
    getRecipesUseCase: GetFilmSimulationRecipesUseCase
) : ViewModel() {

    val uiState: StateFlow<RecipesUiState> =
        getRecipesUseCase.builtIn()
            .catch { emit(emptyList()) }
            .map { recipes -> RecipesUiState.Success(recipes) as RecipesUiState }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipesUiState.Loading)
}
