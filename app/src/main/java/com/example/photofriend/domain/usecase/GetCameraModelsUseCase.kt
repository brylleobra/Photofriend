package com.example.photofriend.domain.usecase

import com.example.photofriend.domain.model.CameraModel
import com.example.photofriend.domain.repository.CameraRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCameraModelsUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    operator fun invoke(): Flow<List<CameraModel>> = repository.getCameraModels()
}
