package com.example.photofriend.data.remote.dto

data class GeminiResponseDto(
    val candidates: List<GeminiCandidateDto>?
)

data class GeminiCandidateDto(
    val content: GeminiContentDto?
)
