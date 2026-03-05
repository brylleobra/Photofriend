package com.example.photofriend.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ClaudeResponseDto(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeResponseContentDto>,
    val model: String,
    @SerializedName("stop_reason") val stopReason: String?,
    val usage: ClaudeUsageDto
)

data class ClaudeResponseContentDto(
    val type: String,
    val text: String?
)

data class ClaudeUsageDto(
    @SerializedName("input_tokens") val inputTokens: Int,
    @SerializedName("output_tokens") val outputTokens: Int
)
