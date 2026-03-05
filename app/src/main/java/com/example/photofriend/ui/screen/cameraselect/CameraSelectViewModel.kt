package com.example.photofriend.ui.screen.cameraselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photofriend.domain.model.CameraModel
import com.example.photofriend.domain.usecase.GetCameraModelsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface CameraSelectUiState {
    object Loading : CameraSelectUiState
    data class Success(val cameras: List<CameraModel>) : CameraSelectUiState
    data class Error(val message: String) : CameraSelectUiState
}

@HiltViewModel
class CameraSelectViewModel @Inject constructor(
    getCameraModelsUseCase: GetCameraModelsUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _cameras: StateFlow<List<CameraModel>> =
        getCameraModelsUseCase()
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<CameraSelectUiState> =
        combine(_cameras, _searchQuery) { cameras, query ->
            val filtered = if (query.isBlank()) {
                cameras
            } else {
                cameras.filter {
                    it.name.contains(query, ignoreCase = true) ||
                        it.brand.contains(query, ignoreCase = true)
                }
            }
            CameraSelectUiState.Success(filtered)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CameraSelectUiState.Loading)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}
