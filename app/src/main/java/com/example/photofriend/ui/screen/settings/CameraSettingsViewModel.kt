package com.example.photofriend.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photofriend.di.SettingsStore
import com.example.photofriend.domain.model.CameraSetting
import com.example.photofriend.domain.model.SettingCategory
import com.example.photofriend.domain.usecase.GetCameraSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

    private val _cameraId = MutableStateFlow("")
    private val _pendingChanges = MutableStateFlow<Map<String, String>>(emptyMap())

    val hasPendingChanges: StateFlow<Boolean> = _pendingChanges
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // Single stable StateFlow — never recreated on recomposition.
    val uiState: StateFlow<CameraSettingsUiState> = _cameraId
        .flatMapLatest { cameraId ->
            if (cameraId.isEmpty()) return@flatMapLatest flowOf(CameraSettingsUiState.Loading)
            combine(
                getCameraSettingsUseCase(cameraId).catch { emit(emptyList()) },
                settingsStore.getValuesFlow(cameraId),
                _pendingChanges
            ) { settings, stored, pending ->
                if (settings.isEmpty()) {
                    CameraSettingsUiState.Loading
                } else {
                    val defaults = settings.associate { it.id to it.defaultValue }
                    CameraSettingsUiState.Success(
                        settingsByCategory = settings.groupBy { it.category },
                        selectedValues = defaults + stored + pending
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CameraSettingsUiState.Loading)

    fun init(cameraId: String) {
        if (_cameraId.value != cameraId) {
            _cameraId.value = cameraId
            _pendingChanges.value = emptyMap()
        }
    }

    /** Stages a change locally; not persisted until [applyChanges] is called. */
    fun onSettingChanged(settingId: String, value: String) {
        _pendingChanges.value = _pendingChanges.value + (settingId to value)
    }

    /** Writes all staged changes to DataStore. */
    fun applyChanges() {
        viewModelScope.launch {
            _pendingChanges.value.forEach { (settingId, value) ->
                settingsStore.setValue(_cameraId.value, settingId, value)
            }
            _pendingChanges.value = emptyMap()
        }
    }

    fun resetAll() {
        viewModelScope.launch {
            _pendingChanges.value = emptyMap()
            settingsStore.resetCamera(_cameraId.value)
        }
    }
}
