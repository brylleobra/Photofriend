package com.example.photofriend.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ClaudeRequestDto(
    val model: String,
    @SerializedName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<ClaudeMessageDto>
)

data class ClaudeMessageDto(
    val role: String,
    val content: List<ClaudeContentDto>
)

data class ClaudeContentDto(
    val type: String,
    val text: String? = null,
    val source: ClaudeImageSourceDto? = null
)

data class ClaudeImageSourceDto(
    val type: String = "base64",
    @SerializedName("media_type") val mediaType: String = "image/jpeg",
    val data: String
)
