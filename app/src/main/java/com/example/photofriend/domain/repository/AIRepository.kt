package com.example.photofriend.domain.repository

import android.graphics.Bitmap
import com.example.photofriend.domain.model.AISuggestion
import com.example.photofriend.domain.model.CameraModel

interface AIRepository {
    suspend fun analyzeScene(bitmap: Bitmap, cameraModel: CameraModel): AISuggestion
}
