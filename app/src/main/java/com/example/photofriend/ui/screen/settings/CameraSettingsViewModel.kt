package com.example.photofriend.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photofriend.di.SettingsStore
import com.example.photofriend.domain.model.CameraSetting
import com.example.photofriend.domain.model.SettingCategory
import com.example.photofriend.domain.usecase.GetCameraSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CameraSettingsUiState {
    object Loading : CameraSettingsUiState
    data class Success(
        val settingsByCategory: Map<SettingCategory, List<CameraSetting>>,
        val selectedValues: Map<String, String>
    ) : CameraSettingsUiState
    data class Error(val message: String) : CameraSettingsUiState
}

@HiltViewModel
class CameraSettingsViewModel @Inject constructor(
    private val getCameraSettingsUseCase: GetCameraSettingsUseCase,
    private val settingsStore: SettingsStore
) : ViewModel() {

    // Retained so onSettingChanged/resetAll don't need the cameraId passed from the screen.
    private var currentCameraId: String = ""

    fun getSettingsFlow(cameraId: String): StateFlow<CameraSettingsUiState> {
        currentCameraId = cameraId
        return combine(
            getCameraSettingsUseCase(cameraId).catch { emit(emptyList()) },
            settingsStore.getValuesFlow(cameraId)
        ) { settings, selected ->
            if (settings.isEmpty()) {
                CameraSettingsUiState.Loading
            } else {
                val defaults = settings.associate { it.id to it.defaultValue }
                CameraSettingsUiState.Success(
                    settingsByCategory = settings.groupBy { it.category },
                    selectedValues = defaults + selected
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CameraSettingsUiState.Loading)
    }

    fun onSettingChanged(settingId: String, value: String) {
        viewModelScope.launch {
            settingsStore.setValue(currentCameraId, settingId, value)
        }
    }

    fun resetAll() {
        viewModelScope.launch {
            settingsStore.resetCamera(currentCameraId)
        }
    }
}
