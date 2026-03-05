package com.example.photofriend.ui.screen.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photofriend.di.SelectedRecipeStore
import com.example.photofriend.domain.model.FilmSimulationRecipe
import com.example.photofriend.domain.usecase.DeleteRecipeUseCase
import com.example.photofriend.domain.usecase.GetFilmSimulationRecipesUseCase
import com.example.photofriend.domain.usecase.SaveRecipeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RecipeTab { BUILT_IN, SAVED }

sealed interface RecipesUiState {
    object Loading : RecipesUiState
    data class Success(
        val builtInRecipes: List<FilmSimulationRecipe>,
        val savedRecipes: List<FilmSimulationRecipe>,
        val activeTab: RecipeTab
    ) : RecipesUiState
}

@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val getRecipesUseCase: GetFilmSimulationRecipesUseCase,
    private val deleteRecipeUseCase: DeleteRecipeUseCase,
    private val saveRecipeUseCase: SaveRecipeUseCase,
    private val selectedRecipeStore: SelectedRecipeStore
) : ViewModel() {

    private val _activeTab = MutableStateFlow(RecipeTab.BUILT_IN)
    val activeTab: StateFlow<RecipeTab> = _activeTab

    val uiState: StateFlow<RecipesUiState> = combine(
        getRecipesUseCase.builtIn().catch { emit(emptyList()) },
        getRecipesUseCase.saved().catch { emit(emptyList()) },
        _activeTab
    ) { builtIn, saved, tab ->
        RecipesUiState.Success(builtIn, saved, tab) as RecipesUiState
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipesUiState.Loading)

    // Holds the last deleted recipe for undo support.
    private var lastDeleted: FilmSimulationRecipe? = null

    fun setTab(tab: RecipeTab) {
        _activeTab.value = tab
    }

    fun selectRecipe(recipe: FilmSimulationRecipe) {
        selectedRecipeStore.select(recipe)
    }

    fun deleteRecipe(recipe: FilmSimulationRecipe) {
        lastDeleted = recipe
        viewModelScope.launch { deleteRecipeUseCase(recipe.id) }
    }

    fun undoDelete() {
        val recipe = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { saveRecipeUseCase(recipe) }
    }
}
