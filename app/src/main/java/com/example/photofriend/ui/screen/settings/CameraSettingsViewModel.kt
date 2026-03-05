package com.example.photofriend.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photofriend.domain.model.CameraSetting
import com.example.photofriend.domain.model.SettingCategory
import com.example.photofriend.domain.usecase.GetCameraSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    private val getCameraSettingsUseCase: GetCameraSettingsUseCase
) : ViewModel() {

    private val _selectedValues = MutableStateFlow<Map<String, String>>(emptyMap())
    val selectedValues: StateFlow<Map<String, String>> = _selectedValues.asStateFlow()

    private var _cameraId = MutableStateFlow<String?>(null)

    fun loadSettings(cameraId: String) {
        _cameraId.value = cameraId
    }

    fun getSettingsFlow(cameraId: String): StateFlow<CameraSettingsUiState> =
        combine(
            getCameraSettingsUseCase(cameraId).catch { emit(emptyList()) },
            _selectedValues
        ) { settings, selected ->
            if (settings.isEmpty()) {
                CameraSettingsUiState.Loading
            } else {
                val defaults = settings.associate { it.id to it.defaultValue }
                val merged = defaults + selected
                CameraSettingsUiState.Success(
                    settingsByCategory = settings.groupBy { it.category },
                    selectedValues = merged
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CameraSettingsUiState.Loading)

    fun onSettingChanged(settingId: String, value: String) {
        _selectedValues.value = _selectedValues.value + (settingId to value)
    }

    fun resetAll() {
        _selectedValues.value = emptyMap()
    }
}
