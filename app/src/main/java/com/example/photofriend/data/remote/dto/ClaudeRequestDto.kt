package com.example.photofriend.data.remote.dto

data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessageDto>,
    val stream: Boolean = false,
    val format: String = "json"
)

data class OllamaMessageDto(
    val role: String,          // "system" | "user" | "assistant"
    val content: String,
    val images: List<String>? = null  // base64-encoded images, no data-URI prefix
)
