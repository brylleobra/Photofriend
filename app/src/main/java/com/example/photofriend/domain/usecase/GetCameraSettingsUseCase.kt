package com.example.photofriend.domain.usecase

import com.example.photofriend.domain.model.CameraSetting
import com.example.photofriend.domain.repository.CameraRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCameraSettingsUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    operator fun invoke(cameraId: String): Flow<List<CameraSetting>> =
        repository.getCameraSettings(cameraId)
}
