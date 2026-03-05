package com.example.photofriend.data.repository

import android.graphics.Bitmap
import android.util.Base64
import com.example.photofriend.data.remote.api.OllamaApiService
import com.example.photofriend.data.remote.dto.OllamaChatRequest
import com.example.photofriend.data.remote.dto.OllamaMessageDto
import com.example.photofriend.di.SettingsStore
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
    private val ollamaApiService: OllamaApiService,
    private val cameraRepository: CameraRepository,
    private val settingsStore: SettingsStore
) : AIRepository {

    private val gson = Gson()

    override suspend fun analyzeScene(bitmap: Bitmap, cameraModel: CameraModel): AISuggestion {
        val settings      = cameraRepository.getCameraSettings(cameraModel.id).first()
        val userSelections = settingsStore.getValuesSnapshot(cameraModel.id)
        val base64Image   = bitmapToBase64(bitmap)

        val request = OllamaChatRequest(
            model = "qwen3-vl:235b-cloud",
            messages = listOf(
                OllamaMessageDto(
                    role    = "system",
                    content = buildSystemPrompt(cameraModel, settings, userSelections)
                ),
                OllamaMessageDto(
                    role   = "user",
                    content = buildUserPrompt(cameraModel),
                    images  = listOf(base64Image)
                )
            )
        )

        val response = ollamaApiService.chat(request)

        val responseText = response.message?.content
            ?: throw Exception("No response from Ollama")

        return parseAISuggestion(responseText, cameraModel.id)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val scaled = scaleBitmap(bitmap, 1024)
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        // Ollama expects raw base64 without the data-URI prefix
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width  = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxOf(width, height)
        return Bitmap.createScaledBitmap(
            bitmap,
            (width * scale).toInt(),
            (height * scale).toInt(),
            true
        )
    }

    private fun buildSystemPrompt(
        camera: CameraModel,
        settings: List<CameraSetting>,
        userSelections: Map<String, String>
    ): String {
        val settingsList = settings.joinToString("\n") { s ->
            val current = userSelections[s.id] ?: s.defaultValue
            "- ${s.name}: [${s.options.joinToString(", ")}] (currently: $current)"
        }
        val selectionNote = if (userSelections.isNotEmpty())
            "The user's current settings are shown above. Factor these preferences into your recipe — keep settings the user has customised unless the scene strongly calls for a change, and explain any overrides in your reasoning."
        else
            "Suggest a complete recipe optimised for this scene."

        return """You are an expert photography assistant specialising in Fujifilm cameras and in-camera JPEG film simulation recipes.

The user has a ${camera.brand} ${camera.name} (${camera.sensorSize} sensor, ${camera.megapixels}MP).

Camera settings with valid options and the user's current selections:
$settingsList

$selectionNote

Respond ONLY with a valid JSON object — no markdown, no text outside the JSON:
{
  "sceneDescription": "one sentence describing the scene and its mood",
  "suggestedSettings": {
    "Film Simulation": "value from options above",
    "Grain Effect": "value from options above",
    "Color Chrome Effect": "value from options above",
    "White Balance": "value from options above",
    "WB Shift R": "integer from -9 to +9",
    "WB Shift B": "integer from -9 to +9",
    "Highlight Tone": "value from options above",
    "Shadow Tone": "value from options above",
    "Color": "value from options above",
    "Sharpness": "value from options above",
    "Noise Reduction": "value from options above",
    "Clarity": "value from options above",
    "Dynamic Range": "value from options above",
    "ISO": "value from options above"
  },
  "filmSimulationRecipeName": "a creative name for this recipe",
  "reasoning": "2-3 sentences explaining why these settings suit this scene"
}

Use only values from the options lists. WB Shift R and WB Shift B are plain integers (e.g. -2, 0, 3).""".trimIndent()
    }

    private fun buildUserPrompt(camera: CameraModel): String =
        "Analyse this scene and suggest the optimal ${camera.brand} ${camera.name} in-camera settings and film simulation recipe. Respond only with the JSON object."

    private fun parseAISuggestion(text: String, cameraId: String): AISuggestion {
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
                cameraId                 = cameraId,
                sceneDescription         = json.get("sceneDescription")?.asString ?: "",
                suggestedSettings        = suggestedSettings,
                filmSimulationRecipeName = json.get("filmSimulationRecipeName")?.asString ?: "AI Recipe",
                reasoning                = json.get("reasoning")?.asString ?: ""
            )
        } catch (e: Exception) {
            AISuggestion(
                cameraId                 = cameraId,
                sceneDescription         = "Scene analyzed",
                suggestedSettings        = emptyMap(),
                filmSimulationRecipeName = "AI Recipe",
                reasoning                = cleaned
            )
        }
    }
}
