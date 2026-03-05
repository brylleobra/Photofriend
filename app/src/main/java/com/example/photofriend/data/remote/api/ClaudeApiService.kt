package com.example.photofriend.data.remote.api

import com.example.photofriend.data.remote.dto.ClaudeRequestDto
import com.example.photofriend.data.remote.dto.ClaudeResponseDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ClaudeApiService {

    @POST("v1/messages")
    suspend fun analyzeScene(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ClaudeRequestDto
    ): ClaudeResponseDto
}
