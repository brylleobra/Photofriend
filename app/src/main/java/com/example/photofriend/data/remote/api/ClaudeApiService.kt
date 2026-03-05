package com.example.photofriend.data.remote.api

import com.example.photofriend.data.remote.dto.OllamaChatRequest
import com.example.photofriend.data.remote.dto.OllamaChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OllamaApiService {

    @POST("api/chat")
    suspend fun chat(
        @Body request: OllamaChatRequest
    ): OllamaChatResponse
}
