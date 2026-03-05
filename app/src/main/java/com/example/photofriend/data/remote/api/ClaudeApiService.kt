package com.example.photofriend.data.remote.api

import com.example.photofriend.data.remote.dto.GeminiRequestDto
import com.example.photofriend.data.remote.dto.GeminiResponseDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApiService {

    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun analyzeScene(
        @Query("key") apiKey: String,
        @Body request: GeminiRequestDto
    ): GeminiResponseDto
}
