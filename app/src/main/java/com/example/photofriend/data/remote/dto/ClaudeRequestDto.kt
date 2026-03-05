package com.example.photofriend.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GeminiRequestDto(
    val contents: List<GeminiContentDto>,
    val systemInstruction: GeminiSystemInstructionDto? = null,
    val generationConfig: GeminiGenerationConfigDto? = null
)

data class GeminiContentDto(
    val parts: List<GeminiPartDto>
)

data class GeminiPartDto(
    val text: String? = null,
    val inlineData: GeminiInlineDataDto? = null
)

data class GeminiInlineDataDto(
    val mimeType: String,
    val data: String
)

data class GeminiSystemInstructionDto(
    val parts: List<GeminiPartDto>
)

data class GeminiGenerationConfigDto(
    val temperature: Float = 0.4f,
    @SerializedName("maxOutputTokens") val maxOutputTokens: Int = 1024,
    val responseMimeType: String = "application/json"
)
