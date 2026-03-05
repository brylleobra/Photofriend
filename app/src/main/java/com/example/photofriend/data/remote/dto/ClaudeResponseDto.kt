package com.example.photofriend.data.remote.dto

data class OllamaChatResponse(
    val model: String?,
    val message: OllamaMessageDto?,
    val done: Boolean?
)
