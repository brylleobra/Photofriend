package com.example.photofriend.data.repository

import android.graphics.Bitmap
import android.util.Base64
import com.example.photofriend.BuildConfig
import com.example.photofriend.data.remote.api.ClaudeApiService
import com.example.photofriend.data.remote.dto.ClaudeContentDto
import com.example.photofriend.data.remote.dto.ClaudeImageSourceDto
import com.example.photofriend.data.remote.dto.ClaudeMessageDto
import com.example.photofriend.data.remote.dto.ClaudeRequestDto
import com.example.photofriend.domain.model.AISuggestion
import com.example.photofriend.domain.model.CameraModel
import com.example.photofriend.domain.model.CameraSetting
import com.example.photofriend.domain.repository.AIRepository
import com.example.photofriend.domain.repository.CameraRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIRepositoryImpl @Inject constructor(
    private val claudeApiService: ClaudeApiService,
    private val cameraRepository: CameraRepository
) : AIRepository {

    private val gson = Gson()

    override suspend fun analyzeScene(bitmap: Bitmap, cameraModel: CameraModel): AISuggestion {
        val settings = cameraRepository.getCameraSettings(cameraModel.id).first()
        val base64Image = bitmapToBase64(bitmap)

        val request = ClaudeRequestDto(
            model = "claude-opus-4-6",
            maxTokens = 1024,
            system = buildSystemPrompt(cameraModel, settings),
            messages = listOf(
                ClaudeMessageDto(
                    role = "user",
                    content = listOf(
                        ClaudeContentDto(
                            type = "image",
                            source = ClaudeImageSourceDto(data = base64Image)
                        ),
                        ClaudeContentDto(
                            type = "text",
                            text = buildUserPrompt(cameraModel)
                        )
                    )
                )
            )
        )

        val response = claudeApiService.analyzeScene(
            apiKey = BuildConfig.CLAUDE_API_KEY,
            request = request
        )

        val responseText = response.content.firstOrNull { it.type == "text" }?.text
            ?: throw Exception("No text response from Claude API")

        return parseAISuggestion(responseText, cameraModel.id)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val scaled = scaleBitmap(bitmap, 1024)
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxOf(width, height)
        return Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
    }

    private fun buildSystemPrompt(camera: CameraModel, settings: List<CameraSetting>): String {
        val settingsList = settings.joinToString("\n") { s ->
            "- ${s.name}: [${s.options.joinToString(", ")}] (default: ${s.defaultValue})"
        }
        return """You are an expert photography assistant specializing in Fujifilm cameras and in-camera JPEG film simulation recipes.

The user has a ${camera.brand} ${camera.name} (${camera.sensorSize} sensor, ${camera.megapixels}MP).

Available camera settings:
$settingsList

Analyze the scene in the photo and respond ONLY with a valid JSON object — no markdown, no explanation outside the JSON:
{
  "sceneDescription": "one sentence describing the scene and its mood",
  "suggestedSettings": {
    "Film Simulation": "value from options above",
    "Grain Effect": "value from options above",
    "Color Chrome Effect": "value from options above",
    "White Balance": "value from options above",
    "Highlight Tone": "value from options above",
    "Shadow Tone": "value from options above",
    "Color": "value from options above",
    "Sharpness": "value from options above",
    "Noise Reduction": "value from options above",
    "Dynamic Range": "value from options above",
    "ISO": "value from options above"
  },
  "filmSimulationRecipeName": "a creative name for this recipe",
  "reasoning": "2-3 sentences explaining why these settings suit this scene"
}

Only use values that appear in the options lists above.""".trimIndent()
    }

    private fun buildUserPrompt(camera: CameraModel): String =
        "Please analyze this scene and suggest the optimal ${camera.brand} ${camera.name} in-camera settings and a film simulation recipe to capture it beautifully."

    private fun parseAISuggestion(text: String, cameraId: String): AISuggestion {
        // Strip markdown code fences if present
        val cleaned = text.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        return try {
            val json = gson.fromJson(cleaned, JsonObject::class.java)
            val suggestedSettings = mutableMapOf<String, String>()
            json.getAsJsonObject("suggestedSettings")?.entrySet()?.forEach { (k, v) ->
                suggestedSettings[k] = v.asString
            }
            AISuggestion(
                cameraId = cameraId,
                sceneDescription = json.get("sceneDescription")?.asString ?: "",
                suggestedSettings = suggestedSettings,
                filmSimulationRecipeName = json.get("filmSimulationRecipeName")?.asString ?: "AI Recipe",
                reasoning = json.get("reasoning")?.asString ?: ""
            )
        } catch (e: Exception) {
            // Graceful fallback: surface the raw text as reasoning
            AISuggestion(
                cameraId = cameraId,
                sceneDescription = "Scene analyzed",
                suggestedSettings = emptyMap(),
                filmSimulationRecipeName = "AI Recipe",
                reasoning = cleaned
            )
        }
    }
}
