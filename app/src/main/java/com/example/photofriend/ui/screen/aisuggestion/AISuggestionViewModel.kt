package com.example.photofriend.ui.screen.aisuggestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photofriend.di.AISuggestionStore
import com.example.photofriend.domain.model.AISuggestion
import com.example.photofriend.domain.model.FilmSimulationRecipe
import com.example.photofriend.domain.usecase.SaveRecipeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface AISuggestionUiState {
    object NoData : AISuggestionUiState
    data class Success(val suggestion: AISuggestion, val savedSuccess: Boolean = false) : AISuggestionUiState
}

@HiltViewModel
class AISuggestionViewModel @Inject constructor(
    private val suggestionStore: AISuggestionStore,
    private val saveRecipeUseCase: SaveRecipeUseCase
) : ViewModel() {

    val uiState: StateFlow<AISuggestionUiState> =
        suggestionStore.suggestion
            .map { suggestion ->
                if (suggestion == null) AISuggestionUiState.NoData
                else AISuggestionUiState.Success(suggestion)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AISuggestionUiState.NoData)

    private val _saveEvent = MutableStateFlow(false)
    val saveEvent: StateFlow<Boolean> = _saveEvent.asStateFlow()

    fun saveAsRecipe() {
        val current = suggestionStore.suggestion.value ?: return
        viewModelScope.launch {
            val recipe = buildRecipeFromSuggestion(current)
            saveRecipeUseCase(recipe)
            _saveEvent.value = true
        }
    }

    fun clearSaveEvent() {
        _saveEvent.value = false
    }

    private fun buildRecipeFromSuggestion(suggestion: AISuggestion): FilmSimulationRecipe {
        val s = suggestion.suggestedSettings
        return FilmSimulationRecipe(
            id = "ai_${UUID.randomUUID()}",
            name = suggestion.filmSimulationRecipeName,
            description = suggestion.sceneDescription,
            filmSimulation = s["Film Simulation"] ?: "Provia/Standard",
            grain = s["Grain Effect"] ?: "Off",
            colorChrome = s["Color Chrome Effect"] ?: "Off",
            colorChromeBlue = s["Color Chrome FX Blue"] ?: "Off",
            whiteBalance = s["White Balance"] ?: "Auto",
            wbShiftR = s["WB Shift R"].toSignedInt(),
            wbShiftB = s["WB Shift B"].toSignedInt(),
            highlights = s["Highlight Tone"].toSignedInt(),
            shadows = s["Shadow Tone"].toSignedInt(),
            color = s["Color"].toSignedInt(),
            sharpness = s["Sharpness"].toSignedInt(),
            noiseReduction = s["Noise Reduction"].toSignedInt(),
            clarity = s["Clarity"].toSignedInt(),
            isoMin = 160,
            isoMax = 3200,
            tags = listOf("ai-generated"),
            isUserSaved = true
        )
    }

    // Handles values like "+1", "-2", "0" that Kotlin's toIntOrNull() rejects for "+"
    private fun String?.toSignedInt(): Int =
        this?.removePrefix("+")?.trim()?.toIntOrNull() ?: 0
}
