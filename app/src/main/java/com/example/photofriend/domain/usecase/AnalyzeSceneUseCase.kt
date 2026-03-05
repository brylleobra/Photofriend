package com.example.photofriend.domain.usecase

import android.graphics.Bitmap
import com.example.photofriend.domain.model.AISuggestion
import com.example.photofriend.domain.model.CameraModel
import com.example.photofriend.domain.repository.AIRepository
import javax.inject.Inject

class AnalyzeSceneUseCase @Inject constructor(
    private val aiRepository: AIRepository
) {
    suspend operator fun invoke(bitmap: Bitmap, cameraModel: CameraModel): AISuggestion =
        aiRepository.analyzeScene(bitmap, cameraModel)
}
