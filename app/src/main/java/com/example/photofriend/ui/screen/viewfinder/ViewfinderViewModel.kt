package com.example.photofriend.ui.screen.viewfinder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photofriend.camera.CameraManager
import com.example.photofriend.di.AISuggestionStore
import com.example.photofriend.domain.model.CameraModel
import com.example.photofriend.domain.repository.CameraRepository
import com.example.photofriend.domain.usecase.AnalyzeSceneUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ViewfinderUiState {
    object Idle : ViewfinderUiState
    object Analyzing : ViewfinderUiState
    object Done : ViewfinderUiState
    data class Error(val message: String) : ViewfinderUiState
}

@HiltViewModel
class ViewfinderViewModel @Inject constructor(
    val cameraManager: CameraManager,
    private val analyzeSceneUseCase: AnalyzeSceneUseCase,
    private val cameraRepository: CameraRepository,
    private val suggestionStore: AISuggestionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<ViewfinderUiState>(ViewfinderUiState.Idle)
    val uiState: StateFlow<ViewfinderUiState> = _uiState.asStateFlow()

    private val _cameraId = MutableStateFlow("")

    val cameraModel: StateFlow<CameraModel?> =
        cameraRepository.getCameraModels()
            .catch { emit(emptyList()) }
            .map { models -> models.find { it.id == _cameraId.value } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun loadCamera(cameraId: String) {
        _cameraId.value = cameraId
    }

    fun analyzeScene() {
        val camera = cameraModel.value ?: run {
            _uiState.value = ViewfinderUiState.Error("Camera model not loaded yet")
            return
        }
        viewModelScope.launch {
            _uiState.value = ViewfinderUiState.Analyzing
            try {
                val bitmap = cameraManager.captureFrame()
                val suggestion = analyzeSceneUseCase(bitmap, camera)
                suggestionStore.store(suggestion)
                _uiState.value = ViewfinderUiState.Done
            } catch (e: Exception) {
                _uiState.value = ViewfinderUiState.Error(e.message ?: "Analysis failed")
            }
        }
    }

    fun resetState() {
        _uiState.value = ViewfinderUiState.Idle
    }
}
