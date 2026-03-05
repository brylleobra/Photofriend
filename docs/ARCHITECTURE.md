# Photofriend — Technical Architecture & Implementation Plan

**Document version**: 1.0
**Date**: 2026-03-05
**Package**: `com.example.photofriend`
**Min SDK**: 24 (Android 7.0) | **Target SDK**: 36 (Android 15)
**Language**: Kotlin 2.0.21 | **JVM target**: Java 11

---

## Table of Contents

1. [Architecture Pattern](#1-architecture-pattern)
2. [Module / Package Structure](#2-module--package-structure)
3. [Key Dependencies](#3-key-dependencies)
4. [Data Models](#4-data-models)
5. [Screen List & Navigation Graph](#5-screen-list--navigation-graph)
6. [CameraX Integration Plan](#6-camerax-integration-plan)
7. [AI Integration Plan](#7-ai-integration-plan)
8. [Camera Settings Database](#8-camera-settings-database)
9. [Film Simulation Recipe Engine](#9-film-simulation-recipe-engine)
10. [Implementation Phases](#10-implementation-phases)

---

## 1. Architecture Pattern

### Recommendation: MVVM + Clean Architecture with UseCases

This combination is the Google-recommended pattern for Jetpack Compose apps and provides the best separation of concerns, testability, and long-term maintainability.

### Layers

```
┌────────────────────────────────────────────────┐
│              UI Layer (Compose)                │
│  Screens · Composables · ViewModels            │
│  State: UiState sealed classes, StateFlow      │
└─────────────────────┬──────────────────────────┘
                      │ calls
┌─────────────────────▼──────────────────────────┐
│             Domain Layer (pure Kotlin)         │
│  UseCases · Domain Models · Repository interfaces│
│  No Android framework imports                  │
└─────────────────────┬──────────────────────────┘
                      │ calls
┌─────────────────────▼──────────────────────────┐
│             Data Layer                         │
│  Repository Implementations · Room DAOs        │
│  Retrofit Services · CameraX wrappers          │
│  Mappers (entity <-> domain model)             │
└────────────────────────────────────────────────┘
```

**UI Layer** — Jetpack Compose screens observe `StateFlow<UiState>` from ViewModels. ViewModels call UseCases and translate results into immutable `UiState` objects. No business logic lives in composables.

**Domain Layer** — Pure Kotlin. Contains interface definitions for repositories so the domain layer never depends on Android or third-party libraries. Each UseCase has a single `invoke()` method, making it trivially testable.

**Data Layer** — Implements repository interfaces. Room handles local persistence; Retrofit + OkHttp handles HTTP to Claude API; CameraX provides the live camera stream. Mapper functions translate between database entities / network DTOs and clean domain models.

**Dependency Injection** — Hilt wires the layers together. `@HiltViewModel` ViewModels receive injected UseCases; UseCases receive injected repositories; repositories receive DAOs and API services.

---

## 2. Module / Package Structure

The app remains a single Gradle module for now. Within it, packages enforce layer boundaries.

```
com.example.photofriend
│
├── di/
│   ├── DatabaseModule.kt          # Room DB + DAO providers
│   ├── NetworkModule.kt           # OkHttp, Retrofit, Claude API service
│   ├── CameraModule.kt            # CameraX use-case providers
│   └── RepositoryModule.kt        # Binds interfaces to implementations
│
├── domain/
│   ├── model/
│   │   ├── CameraModel.kt
│   │   ├── CameraSetting.kt
│   │   ├── FilmSimulationRecipe.kt
│   │   └── AISuggestion.kt
│   ├── repository/
│   │   ├── CameraModelRepository.kt     # interface
│   │   ├── RecipeRepository.kt          # interface
│   │   └── AIRepository.kt              # interface
│   └── usecase/
│       ├── GetCameraModelsUseCase.kt
│       ├── GetCameraSettingsUseCase.kt
│       ├── GetFilmSimulationsUseCase.kt
│       ├── AnalyzeSceneUseCase.kt
│       ├── SaveRecipeUseCase.kt
│       └── GetSavedRecipesUseCase.kt
│
├── data/
│   ├── local/
│   │   ├── db/
│   │   │   ├── PhotofriendDatabase.kt   # Room DB
│   │   │   ├── dao/
│   │   │   │   ├── CameraModelDao.kt
│   │   │   │   ├── CameraSettingDao.kt
│   │   │   │   └── RecipeDao.kt
│   │   │   └── entity/
│   │   │       ├── CameraModelEntity.kt
│   │   │       ├── CameraSettingEntity.kt
│   │   │       └── RecipeEntity.kt
│   │   └── seed/
│   │       ├── CameraSeeds.kt           # Hard-coded Fujifilm camera data
│   │       └── RecipeSeeds.kt           # Built-in film simulation recipes
│   ├── remote/
│   │   ├── api/
│   │   │   └── ClaudeApiService.kt      # Retrofit interface
│   │   └── dto/
│   │       ├── ClaudeRequestDto.kt
│   │       └── ClaudeResponseDto.kt
│   ├── mapper/
│   │   ├── CameraMapper.kt
│   │   └── RecipeMapper.kt
│   └── repository/
│       ├── CameraModelRepositoryImpl.kt
│       ├── RecipeRepositoryImpl.kt
│       └── AIRepositoryImpl.kt
│
├── camera/
│   ├── CameraManager.kt             # CameraX lifecycle wrapper
│   ├── FrameAnalyzer.kt             # ImageAnalysis.Analyzer impl
│   └── BitmapUtils.kt               # YUV -> Bitmap conversion helpers
│
├── ui/
│   ├── theme/
│   │   ├── Color.kt                 # existing
│   │   ├── Theme.kt                 # existing
│   │   └── Type.kt                  # existing
│   ├── navigation/
│   │   ├── NavGraph.kt              # NavHost + composable routes
│   │   └── Screen.kt                # sealed class of route strings
│   ├── screen/
│   │   ├── cameraselect/
│   │   │   ├── CameraSelectScreen.kt
│   │   │   └── CameraSelectViewModel.kt
│   │   ├── viewfinder/
│   │   │   ├── ViewfinderScreen.kt
│   │   │   └── ViewfinderViewModel.kt
│   │   ├── settings/
│   │   │   ├── CameraSettingsScreen.kt
│   │   │   └── CameraSettingsViewModel.kt
│   │   ├── aisuggestion/
│   │   │   ├── AISuggestionScreen.kt
│   │   │   └── AISuggestionViewModel.kt
│   │   └── savedrecipes/
│   │       ├── SavedRecipesScreen.kt
│   │       └── SavedRecipesViewModel.kt
│   └── component/
│       ├── CameraPreview.kt         # AndroidView wrapper for PreviewView
│       ├── SettingSlider.kt
│       ├── SettingChip.kt
│       └── RecipeCard.kt
│
└── MainActivity.kt                  # existing, hosts NavHost
```

---

## 3. Key Dependencies

All additions go into `gradle/libs.versions.toml` first, then referenced in `app/build.gradle.kts`.

### 3.1 Updated `gradle/libs.versions.toml`

```toml
[versions]
# --- existing ---
agp                  = "9.0.1"
kotlin               = "2.0.21"
composeBom           = "2024.09.00"
coreKtx              = "1.10.1"
lifecycleRuntimeKtx  = "2.6.1"
activityCompose      = "1.8.0"
junit                = "4.13.2"
junitVersion         = "1.1.5"
espressoCore         = "3.5.1"

# --- new ---
cameraX              = "1.4.2"
hilt                 = "2.52"
hiltNavigationCompose = "1.2.0"
room                 = "2.6.1"
retrofit             = "2.11.0"
okhttp               = "4.12.0"
kotlinxCoroutines    = "1.8.1"
kotlinxSerializationJson = "1.7.3"
coil                 = "2.7.0"
navigationCompose    = "2.7.7"
lifecycleViewmodelCompose = "2.8.7"
datastorePrefs       = "1.1.1"

[libraries]
# --- existing ---
androidx-core-ktx                  = { group = "androidx.core",          name = "core-ktx",                   version.ref = "coreKtx" }
junit                              = { group = "junit",                   name = "junit",                      version.ref = "junit" }
androidx-junit                     = { group = "androidx.test.ext",       name = "junit",                      version.ref = "junitVersion" }
androidx-espresso-core             = { group = "androidx.test.espresso",  name = "espresso-core",              version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx     = { group = "androidx.lifecycle",      name = "lifecycle-runtime-ktx",      version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose          = { group = "androidx.activity",        name = "activity-compose",           version.ref = "activityCompose" }
androidx-compose-bom               = { group = "androidx.compose",         name = "compose-bom",                version.ref = "composeBom" }
androidx-compose-ui                = { group = "androidx.compose.ui",      name = "ui" }
androidx-compose-ui-graphics       = { group = "androidx.compose.ui",      name = "ui-graphics" }
androidx-compose-ui-tooling        = { group = "androidx.compose.ui",      name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui",     name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest  = { group = "androidx.compose.ui",      name = "ui-test-manifest" }
androidx-compose-ui-test-junit4    = { group = "androidx.compose.ui",      name = "ui-test-junit4" }
androidx-compose-material3         = { group = "androidx.compose.material3", name = "material3" }

# --- CameraX ---
camerax-core         = { group = "androidx.camera", name = "camera-core",      version.ref = "cameraX" }
camerax-camera2      = { group = "androidx.camera", name = "camera-camera2",   version.ref = "cameraX" }
camerax-lifecycle    = { group = "androidx.camera", name = "camera-lifecycle",  version.ref = "cameraX" }
camerax-view         = { group = "androidx.camera", name = "camera-view",       version.ref = "cameraX" }

# --- Hilt ---
hilt-android         = { group = "com.google.dagger", name = "hilt-android",           version.ref = "hilt" }
hilt-compiler        = { group = "com.google.dagger", name = "hilt-android-compiler",  version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

# --- Room ---
room-runtime         = { group = "androidx.room", name = "room-runtime",   version.ref = "room" }
room-ktx             = { group = "androidx.room", name = "room-ktx",       version.ref = "room" }
room-compiler        = { group = "androidx.room", name = "room-compiler",  version.ref = "room" }

# --- Retrofit + OkHttp ---
retrofit-core        = { group = "com.squareup.retrofit2", name = "retrofit",                      version.ref = "retrofit" }
retrofit-gson        = { group = "com.squareup.retrofit2", name = "converter-gson",                version.ref = "retrofit" }
okhttp-core          = { group = "com.squareup.okhttp3",   name = "okhttp",                        version.ref = "okhttp" }
okhttp-logging       = { group = "com.squareup.okhttp3",   name = "logging-interceptor",           version.ref = "okhttp" }

# --- Coroutines ---
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinxCoroutines" }

# --- Serialization ---
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }

# --- Coil ---
coil-compose         = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

# --- Navigation ---
navigation-compose   = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }

# --- Lifecycle ViewModel Compose ---
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }

# --- DataStore ---
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastorePrefs" }

[plugins]
android-application  = { id = "com.android.application",              version.ref = "agp" }
kotlin-compose       = { id = "org.jetbrains.kotlin.plugin.compose",  version.ref = "kotlin" }
kotlin-android       = { id = "org.jetbrains.kotlin.android",         version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt-android         = { id = "com.google.dagger.hilt.android",       version.ref = "hilt" }
ksp                  = { id = "com.google.devtools.ksp",              version = "2.0.21-1.0.27" }
```

### 3.2 `app/build.gradle.kts` additions

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

// inside android { ... }
buildFeatures {
    compose = true
    buildConfig = true          // needed to expose CLAUDE_API_KEY
}

// in defaultConfig { ... }
buildConfigField("String", "CLAUDE_API_KEY", "\"${properties["claude.api.key"] ?: ""}\"")

// dependencies block additions:
dependencies {
    // ... existing deps ...

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit + OkHttp
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coil
    implementation(libs.coil.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle ViewModel Compose
    implementation(libs.lifecycle.viewmodel.compose)

    // DataStore
    implementation(libs.datastore.preferences)
}
```

> **API Key storage**: Store `claude.api.key=sk-ant-...` in `local.properties` (already git-ignored). `BuildConfig.CLAUDE_API_KEY` makes it available at runtime without committing secrets.

---

## 4. Data Models

### 4.1 Domain Models (`domain/model/`)

```kotlin
// CameraModel.kt
data class CameraModel(
    val id: Long = 0,
    val brand: String,           // e.g. "Fujifilm"
    val name: String,            // e.g. "X-T30 III"
    val sensorType: String,      // e.g. "X-Trans CMOS 5 HR"
    val megapixels: Float,       // e.g. 40.2
    val hasIBIS: Boolean,
    val releaseYear: Int,
    val imageUrl: String? = null // optional product image URL
)

// CameraSetting.kt
data class CameraSetting(
    val id: Long = 0,
    val cameraModelId: Long,
    val category: SettingCategory,
    val name: String,            // e.g. "Film Simulation"
    val key: String,             // stable identifier, e.g. "film_simulation"
    val type: SettingType,
    val options: List<String>,   // for ENUM type: ["Provia", "Velvia", ...]
    val minValue: Float? = null, // for RANGE type
    val maxValue: Float? = null,
    val stepValue: Float? = null,
    val defaultValue: String,
    val unit: String? = null,    // e.g. "EV", "K" for kelvin
    val description: String? = null
)

enum class SettingCategory {
    EXPOSURE, COLOR, FOCUS, FILM_SIMULATION, NOISE_REDUCTION, SHARPENING, DYNAMIC_RANGE, WHITE_BALANCE, ADVANCED
}

enum class SettingType {
    ENUM,    // discrete list of named options
    RANGE,   // continuous float range with step
    BOOLEAN, // on/off toggle
    INTEGER  // whole-number range (e.g. ISO steps)
}

// FilmSimulationRecipe.kt
data class FilmSimulationRecipe(
    val id: Long = 0,
    val name: String,                       // e.g. "Golden Hour Velvia"
    val description: String,
    val baseSimulation: String,             // e.g. "Velvia/Vivid"
    val settings: Map<String, String>,      // key -> value pairs
    val tags: List<String>,                 // e.g. ["landscape", "golden hour", "warm"]
    val thumbnailUrl: String? = null,
    val isUserCreated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val source: RecipeSource = RecipeSource.BUILT_IN
)

enum class RecipeSource { BUILT_IN, AI_GENERATED, USER_CREATED }

// AISuggestion.kt
data class AISuggestion(
    val id: Long = 0,
    val sceneDescription: String,           // Claude's analysis of the scene
    val suggestedSettings: Map<String, String>, // setting key -> suggested value
    val suggestedRecipes: List<FilmSimulationRecipe>,
    val reasoning: String,                  // Claude's explanation
    val confidence: Float,                  // 0.0 - 1.0
    val cameraModelId: Long,
    val createdAt: Long = System.currentTimeMillis()
)
```

---

## 5. Screen List & Navigation Graph

### 5.1 Screen Sealed Class

```kotlin
// ui/navigation/Screen.kt
sealed class Screen(val route: String) {
    object CameraSelect   : Screen("camera_select")
    object Viewfinder     : Screen("viewfinder/{cameraModelId}") {
        fun createRoute(cameraModelId: Long) = "viewfinder/$cameraModelId"
    }
    object CameraSettings : Screen("camera_settings/{cameraModelId}") {
        fun createRoute(cameraModelId: Long) = "camera_settings/$cameraModelId"
    }
    object AISuggestion   : Screen("ai_suggestion/{cameraModelId}") {
        fun createRoute(cameraModelId: Long) = "ai_suggestion/$cameraModelId"
    }
    object SavedRecipes   : Screen("saved_recipes")
    object RecipeDetail   : Screen("recipe_detail/{recipeId}") {
        fun createRoute(recipeId: Long) = "recipe_detail/$recipeId"
    }
}
```

### 5.2 Navigation Graph

```kotlin
// ui/navigation/NavGraph.kt
@Composable
fun PhotofriendNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.CameraSelect.route
    ) {
        composable(Screen.CameraSelect.route) {
            CameraSelectScreen(
                onCameraSelected = { cameraModelId ->
                    navController.navigate(Screen.Viewfinder.createRoute(cameraModelId))
                }
            )
        }
        composable(
            route = Screen.Viewfinder.route,
            arguments = listOf(navArgument("cameraModelId") { type = NavType.LongType })
        ) { backStackEntry ->
            val cameraModelId = backStackEntry.arguments?.getLong("cameraModelId") ?: return@composable
            ViewfinderScreen(
                cameraModelId = cameraModelId,
                onOpenSettings = { navController.navigate(Screen.CameraSettings.createRoute(cameraModelId)) },
                onAnalyzeScene = { navController.navigate(Screen.AISuggestion.createRoute(cameraModelId)) },
                onOpenSavedRecipes = { navController.navigate(Screen.SavedRecipes.route) }
            )
        }
        composable(Screen.CameraSettings.route, /* ... */) { /* ... */ }
        composable(Screen.AISuggestion.route,   /* ... */) { /* ... */ }
        composable(Screen.SavedRecipes.route)   { /* ... */ }
        composable(Screen.RecipeDetail.route,   /* ... */) { /* ... */ }
    }
}
```

### 5.3 Screen Descriptions

| Screen | Route | Purpose |
|---|---|---|
| **CameraSelectScreen** | `camera_select` | Browse and select a Fujifilm camera model. Shows brand logo, sensor info, and a search field. |
| **ViewfinderScreen** | `viewfinder/{cameraModelId}` | Full-screen CameraX live preview with a floating bottom sheet showing current suggested settings. FAB triggers AI analysis. |
| **CameraSettingsScreen** | `camera_settings/{cameraModelId}` | Scrollable list of all settings for the selected camera, grouped by `SettingCategory`. Tapping a setting opens an inline editor. |
| **AISuggestionScreen** | `ai_suggestion/{cameraModelId}` | Displays Claude's scene analysis, recommended settings overlay, and matched film simulation recipes. "Apply" button updates the settings state. |
| **SavedRecipesScreen** | `saved_recipes` | Grid of saved recipes (both built-in and AI-generated). Filter chips by tag. |
| **RecipeDetailScreen** | `recipe_detail/{recipeId}` | Full settings breakdown for a single recipe with a share button. |

---

## 6. CameraX Integration Plan

### 6.1 Required Permissions

Add to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<!-- Optional, for saving captured frames: -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

Request `CAMERA` at runtime with the Accompanist Permissions library or the built-in `rememberPermissionState` from `accompanist-permissions`.

### 6.2 `CameraManager.kt`

```kotlin
@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onFrameCaptured: (Bitmap) -> Unit
    ) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        FrameAnalyzer(onFrameCaptured)
                    )
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        }, ContextCompat.getMainExecutor(context))
    }
}
```

### 6.3 `FrameAnalyzer.kt`

```kotlin
class FrameAnalyzer(
    private val onFrameReady: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    // Throttle: only forward a new frame every 500 ms to avoid flooding the AI
    private var lastAnalysisTimestamp = 0L

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTimestamp >= 500L) {
            lastAnalysisTimestamp = currentTime
            imageProxy.toBitmap()?.let { onFrameReady(it) }
        }
        imageProxy.close()
    }
}
```

### 6.4 Compose Integration — `CameraPreview.kt`

```kotlin
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraManager: CameraManager,
    onFrameCaptured: (Bitmap) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                cameraManager.bindCamera(lifecycleOwner, this, onFrameCaptured)
            }
        }
    )
}
```

### 6.5 Capturing a Single Frame for AI Analysis

When the user taps the "Analyze" FAB, the ViewfinderViewModel latches the most recent `Bitmap` from `onFrameCaptured`. This bitmap is then converted to a Base64 PNG string and forwarded to the AI repository:

```kotlin
// In ViewfinderViewModel
private var latestFrame: Bitmap? = null

fun onFrameCaptured(bitmap: Bitmap) {
    latestFrame = bitmap   // always kept fresh by the analyzer
}

fun requestAIAnalysis() {
    val frame = latestFrame ?: return
    viewModelScope.launch {
        _uiState.update { it.copy(isAnalyzing = true) }
        analyzeSceneUseCase(frame, selectedCameraModelId)
            .onSuccess { suggestion ->
                _uiState.update { it.copy(suggestion = suggestion, isAnalyzing = false) }
            }
            .onFailure { err ->
                _uiState.update { it.copy(error = err.message, isAnalyzing = false) }
            }
    }
}
```

---

## 7. AI Integration Plan

### 7.1 API Choice

Photofriend calls the **Anthropic Messages API** directly via Retrofit over HTTPS. The API supports vision (image input) in `claude-3-5-sonnet-20241022` and later models, which is required for scene analysis.

No official Anthropic Android SDK exists as of this writing; the REST API is straightforward and well-documented.

**Base URL**: `https://api.anthropic.com/v1/`

### 7.2 Network Layer

```kotlin
// data/remote/api/ClaudeApiService.kt
interface ClaudeApiService {
    @POST("messages")
    suspend fun analyzeScene(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") apiVersion: String = "2023-06-01",
        @Body request: ClaudeRequestDto
    ): ClaudeResponseDto
}
```

```kotlin
// data/remote/dto/ClaudeRequestDto.kt
data class ClaudeRequestDto(
    @SerializedName("model")      val model: String = "claude-opus-4-6",
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    @SerializedName("messages")   val messages: List<MessageDto>
)

data class MessageDto(
    @SerializedName("role")    val role: String,   // "user"
    @SerializedName("content") val content: List<ContentBlockDto>
)

sealed class ContentBlockDto {
    data class Text(
        @SerializedName("type") val type: String = "text",
        @SerializedName("text") val text: String
    ) : ContentBlockDto()

    data class Image(
        @SerializedName("type") val type: String = "image",
        @SerializedName("source") val source: ImageSourceDto
    ) : ContentBlockDto()
}

data class ImageSourceDto(
    @SerializedName("type")       val type: String = "base64",
    @SerializedName("media_type") val mediaType: String = "image/jpeg",
    @SerializedName("data")       val data: String  // Base64-encoded JPEG
)
```

```kotlin
// data/remote/dto/ClaudeResponseDto.kt
data class ClaudeResponseDto(
    @SerializedName("id")      val id: String,
    @SerializedName("content") val content: List<ResponseContentDto>,
    @SerializedName("usage")   val usage: UsageDto
)

data class ResponseContentDto(
    @SerializedName("type") val type: String,
    @SerializedName("text") val text: String
)

data class UsageDto(
    @SerializedName("input_tokens")  val inputTokens: Int,
    @SerializedName("output_tokens") val outputTokens: Int
)
```

### 7.3 Image Preparation

To keep request sizes manageable, compress the captured `Bitmap` to a JPEG at 80% quality and scale it down to a maximum of 1024px on the longest side before Base64 encoding:

```kotlin
// camera/BitmapUtils.kt
object BitmapUtils {
    fun prepareForUpload(source: Bitmap, maxDimension: Int = 1024): String {
        val scale = maxDimension.toFloat() / maxOf(source.width, source.height)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt(),
                (source.height * scale).toInt(),
                true
            )
        } else source

        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}
```

### 7.4 Prompt Engineering

The prompt is assembled in `AIRepositoryImpl` from three components:

1. **System prompt** — establishes Claude as a photography expert familiar with Fujifilm in-camera settings.
2. **Camera context** — the selected camera model name and its full settings list (keys + valid options).
3. **Task instruction** — asks for a structured JSON response to simplify parsing.

```kotlin
private fun buildSystemPrompt(camera: CameraModel, settings: List<CameraSetting>): String = """
You are an expert film photographer and Fujifilm camera specialist with deep knowledge of
in-camera settings, film simulation modes, and how they interact with real-world lighting
conditions.

The user is shooting with a ${camera.brand} ${camera.name}.

Available camera settings for this model:
${settings.joinToString("\n") { s ->
    "- ${s.name} (key: ${s.key}): ${
        when (s.type) {
            SettingType.ENUM    -> "options: ${s.options.joinToString(", ")}"
            SettingType.RANGE   -> "range: ${s.minValue}..${s.maxValue} step ${s.stepValue} ${s.unit ?: ""}"
            SettingType.BOOLEAN -> "on/off"
            SettingType.INTEGER -> "range: ${s.minValue?.toInt()}..${s.maxValue?.toInt()}"
        }
    }"
}}

Analyze the provided scene image and respond ONLY with a valid JSON object matching this schema:
{
  "scene_description": "string — one sentence describing what you see",
  "lighting_conditions": "string — e.g. 'harsh midday sun', 'blue hour', 'overcast diffuse'",
  "suggested_settings": { "<setting_key>": "<value>", ... },
  "film_simulation_name": "string — name for this recipe",
  "film_simulation_description": "string — mood and intent",
  "reasoning": "string — why these settings suit this scene",
  "confidence": 0.0
}

Be concise. Only include settings that you are meaningfully adjusting from their defaults.
""".trimIndent()
```

### 7.5 Response Parsing

Claude's text response is parsed from JSON into an `AISuggestion` domain object:

```kotlin
// data/repository/AIRepositoryImpl.kt
override suspend fun analyzeScene(
    frame: Bitmap,
    camera: CameraModel,
    settings: List<CameraSetting>
): Result<AISuggestion> = runCatching {
    val base64Image = BitmapUtils.prepareForUpload(frame)
    val systemPrompt = buildSystemPrompt(camera, settings)

    val request = ClaudeRequestDto(
        messages = listOf(
            MessageDto(
                role = "user",
                content = listOf(
                    ContentBlockDto.Image(source = ImageSourceDto(data = base64Image)),
                    ContentBlockDto.Text(text = systemPrompt)
                )
            )
        )
    )

    val response = apiService.analyzeScene(
        apiKey = BuildConfig.CLAUDE_API_KEY,
        request = request
    )

    val json = response.content.first { it.type == "text" }.text
    parseAISuggestion(json, camera.id)
}

private fun parseAISuggestion(json: String, cameraModelId: Long): AISuggestion {
    val obj = JSONObject(json)
    val settingsObj = obj.getJSONObject("suggested_settings")
    val suggestedSettings = settingsObj.keys().asSequence()
        .associateWith { settingsObj.getString(it) }

    val recipe = FilmSimulationRecipe(
        name        = obj.getString("film_simulation_name"),
        description = obj.getString("film_simulation_description"),
        baseSimulation = suggestedSettings["film_simulation"] ?: "Provia/Standard",
        settings    = suggestedSettings,
        tags        = listOf("ai-generated"),
        source      = RecipeSource.AI_GENERATED
    )

    return AISuggestion(
        sceneDescription  = obj.getString("scene_description"),
        suggestedSettings = suggestedSettings,
        suggestedRecipes  = listOf(recipe),
        reasoning         = obj.getString("reasoning"),
        confidence        = obj.getDouble("confidence").toFloat(),
        cameraModelId     = cameraModelId
    )
}
```

### 7.6 Error Handling

- Wrap every API call in `runCatching { ... }` and propagate `Result<T>` to the ViewModel.
- Surface `HttpException` (4xx/5xx) and `IOException` (network) as distinct UI error states.
- Rate-limit user-triggered analyses to one request per 5 seconds via a `Mutex` + timestamp guard in the ViewModel.

---

## 8. Camera Settings Database

### 8.1 Room Setup

```kotlin
// data/local/db/PhotofriendDatabase.kt
@Database(
    entities = [CameraModelEntity::class, CameraSettingEntity::class, RecipeEntity::class],
    version = 1,
    exportSchema = true
)
abstract class PhotofriendDatabase : RoomDatabase() {
    abstract fun cameraModelDao(): CameraModelDao
    abstract fun cameraSettingDao(): CameraSettingDao
    abstract fun recipeDao(): RecipeDao
}
```

```kotlin
// di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PhotofriendDatabase =
        Room.databaseBuilder(context, PhotofriendDatabase::class.java, "photofriend.db")
            .addCallback(SeedDatabaseCallback(context))
            .build()

    @Provides fun provideCameraModelDao(db: PhotofriendDatabase) = db.cameraModelDao()
    @Provides fun provideCameraSettingDao(db: PhotofriendDatabase) = db.cameraSettingDao()
    @Provides fun provideRecipeDao(db: PhotofriendDatabase) = db.recipeDao()
}
```

### 8.2 Seed Strategy

On first run, a `RoomDatabase.Callback.onCreate()` inserts seed data from hardcoded Kotlin objects. This avoids a network round-trip and works offline.

```kotlin
class SeedDatabaseCallback(
    private val context: Context
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Run on IO dispatcher — Room callback runs on the main thread by default
        CoroutineScope(Dispatchers.IO).launch {
            val database = PhotofriendDatabase.getInstance(context)
            database.cameraModelDao().insertAll(CameraSeeds.models)
            CameraSeeds.settingsByModelKey.forEach { (modelKey, settings) ->
                val modelId = database.cameraModelDao().getByKey(modelKey)?.id ?: return@forEach
                database.cameraSettingDao().insertAll(settings.map { it.copy(cameraModelId = modelId) })
            }
            database.recipeDao().insertAll(RecipeSeeds.recipes)
        }
    }
}
```

### 8.3 Fujifilm X-T30 III Setting Seed Sample (`data/local/seed/CameraSeeds.kt`)

```kotlin
object CameraSeeds {
    val models = listOf(
        CameraModelEntity(
            key         = "fujifilm_xt30iii",
            brand       = "Fujifilm",
            name        = "X-T30 III",
            sensorType  = "X-Trans CMOS 5 HR",
            megapixels  = 40.2f,
            hasIBIS     = false,
            releaseYear = 2024
        ),
        // ... additional models (X-T5, X100VI, X-S20, GFX100S II, etc.)
    )

    // Settings for X-T30 III (model key used as grouping key before IDs are assigned)
    val settingsByModelKey = mapOf(
        "fujifilm_xt30iii" to listOf(
            CameraSettingEntity(
                cameraModelId = 0, // filled in during seed
                category      = "FILM_SIMULATION",
                name          = "Film Simulation",
                key           = "film_simulation",
                type          = "ENUM",
                options       = "Provia/Standard,Velvia/Vivid,Astia/Soft,Classic Chrome,Reala Ace," +
                                "Pro Neg. Hi,Pro Neg. Std,Classic Neg.,Nostalgic Neg.,Eterna/Cinema," +
                                "Eterna Bleach Bypass,Acros,Monochrome,Sepia",
                defaultValue  = "Provia/Standard"
            ),
            CameraSettingEntity(
                cameraModelId = 0,
                category      = "COLOR",
                name          = "Grain Effect",
                key           = "grain_effect",
                type          = "ENUM",
                options       = "Off,Weak Small,Weak Large,Strong Small,Strong Large",
                defaultValue  = "Off"
            ),
            CameraSettingEntity(
                cameraModelId = 0,
                category      = "COLOR",
                name          = "Color Chrome Effect",
                key           = "color_chrome_effect",
                type          = "ENUM",
                options       = "Off,Weak,Strong",
                defaultValue  = "Off"
            ),
            CameraSettingEntity(
                cameraModelId = 0,
                category      = "COLOR",
                name          = "Color Chrome FX Blue",
                key           = "color_chrome_fx_blue",
                type          = "ENUM",
                options       = "Off,Weak,Strong",
                defaultValue  = "Off"
            ),
            CameraSettingEntity(
                cameraModelId = 0,
                category      = "WHITE_BALANCE",
                name          = "White Balance",
                key           = "white_balance",
                type          = "ENUM",
                options       = "Auto,Daylight,Shade,Fluorescent 1,Fluorescent 2,Fluorescent 3,Incandescent,Underwater,Custom 1,Custom 2,Color Temp.",
                defaultValue  = "Auto"
            ),
            CameraSettingEntity(
                cameraModelId = 0,
                category      = "WHITE_BALANCE",
                name          = "White Balance Shift Red",
                key           = "wb_shift_red",
                type          = "RANGE",
                options       = "",
                minValue      = -9f, maxValue = 9f, stepValue = 1f,
                defaultValue  = "0",
                unit          = ""
            ),
            CameraSettingEntity(
                cameraModelId = 0,
                category      = "WHITE_BALANCE",
                name          = "White Balance Shift Blue",
                key           = "wb_shift_blue",
                type          = "RANGE",
                options       = "",
                minValue      = -9f, maxValue = 9f, stepValue = 1f,
                defaultValue  = "0"
            ),
            CameraSettingEntity(
                cameraModelId = 0,
                category      = "COLOR",
                name          = "Highlight Tone",
                key           = "highlight_tone",
                type          = "RANGE",
                options       = "",
                minValue      = -2f, maxValue = 4f, stepValue = 1f,
                defaultValue  = "0"
            ),
            CameraSettingEntity(
                cameraModelId = 0,
                category      = "COLOR",
                name          = "Shadow Tone",
                key           = "shadow_tone",
                type          = "RANGE",
                options       = "",
                minValue      = -2f, maxValue = 4f, stepValue = 1f,
                defaultValue  = "0"
            ),
            CameraSettingEntity(
                cameraModelId = 0,
                category      = "COLOR",
                name          = "Color Saturation",
                key           = "color",
                type          = "RANGE",
                options       = "",
                minValue      = -4f, maxValue = 4f, stepValue = 1f,
                defaultValue  = "0"
            ),
            CameraSettingEntity(
                cameraModelId = 0,
                category      = "SHARPENING",
                name          = "Sharpness",
                key           = "sharpness",
                type          = "RANGE",
                options       = "",
                minValue      = -4f, maxValue = 4f, stepValue = 1f,
                defaultValue  = "0"
            ),
            CameraSettingEntity(
                cameraModelId = 0,
                category      = "NOISE_REDUCTION",
                name          = "Noise Reduction",
                key           = "noise_reduction",
                type          = "RANGE",
                options       = "",
                minValue      = -4f, maxValue = 4f, stepValue = 1f,
                defaultValue  = "0"
            ),
            CameraSettingEntity(
                cameraModelId = 0,
                category      = "DYNAMIC_RANGE",
                name          = "Dynamic Range",
                key           = "dynamic_range",
                type          = "ENUM",
                options       = "Auto,DR100,DR200,DR400",
                defaultValue  = "DR100"
            ),
            CameraSettingEntity(
                cameraModelId = 0,
                category      = "DYNAMIC_RANGE",
                name          = "D-Range Priority",
                key           = "d_range_priority",
                type          = "ENUM",
                options       = "Off,Auto,Weak,Strong",
                defaultValue  = "Off"
            ),
            CameraSettingEntity(
                cameraModelId = 0,
                category      = "EXPOSURE",
                name          = "ISO",
                key           = "iso",
                type          = "ENUM",
                options       = "Auto,L (80),160,200,400,800,1600,3200,6400,12800,25600,51200",
                defaultValue  = "Auto"
            ),
            CameraSettingEntity(
                cameraModelId = 0,
                category      = "EXPOSURE",
                name          = "Exposure Compensation",
                key           = "exposure_compensation",
                type          = "RANGE",
                options       = "",
                minValue      = -5f, maxValue = 5f, stepValue = 0.33f,
                defaultValue  = "0",
                unit          = "EV"
            ),
            CameraSettingEntity(
                cameraModelId = 0,
                category      = "ADVANCED",
                name          = "Clarity",
                key           = "clarity",
                type          = "RANGE",
                options       = "",
                minValue      = -5f, maxValue = 5f, stepValue = 1f,
                defaultValue  = "0"
            )
        )
    )
}
```

---

## 9. Film Simulation Recipe Engine

### 9.1 Data Model Recap

A `FilmSimulationRecipe` (see Section 4.1) stores:
- A human-readable `name` and `description`
- The base `Film Simulation` mode
- A `Map<String, String>` of setting key/value overrides
- Freeform `tags` for filtering
- `source` to distinguish built-in, AI-generated, and user-created recipes

### 9.2 Recipe Repository

```kotlin
// domain/repository/RecipeRepository.kt
interface RecipeRepository {
    fun getAllRecipes(): Flow<List<FilmSimulationRecipe>>
    fun getRecipesByTag(tag: String): Flow<List<FilmSimulationRecipe>>
    suspend fun getRecipeById(id: Long): FilmSimulationRecipe?
    suspend fun saveRecipe(recipe: FilmSimulationRecipe): Long
    suspend fun deleteRecipe(id: Long)
}
```

### 9.3 Seed Recipes (`data/local/seed/RecipeSeeds.kt`)

```kotlin
object RecipeSeeds {
    val recipes = listOf(

        RecipeEntity(
            name           = "Vintage Kodachrome",
            description    = "Warm, punchy colors reminiscent of slide film from the 1970s. Great for street and travel.",
            baseSimulation = "Velvia/Vivid",
            settingsJson   = """{"film_simulation":"Velvia/Vivid","color":"2","highlight_tone":"-1","shadow_tone":"1","sharpness":"1","noise_reduction":"-2","wb_shift_red":"3","wb_shift_blue":"-2","grain_effect":"Weak Small"}""",
            tagsJson       = """["street","travel","warm","vintage","colorful"]""",
            source         = "BUILT_IN"
        ),

        RecipeEntity(
            name           = "Moody Monochrome",
            description    = "Deep shadows and rich grain for a classic documentary black-and-white look.",
            baseSimulation = "Acros",
            settingsJson   = """{"film_simulation":"Acros","grain_effect":"Strong Large","sharpness":"2","highlight_tone":"-1","shadow_tone":"-2","noise_reduction":"-4","clarity":"2"}""",
            tagsJson       = """["monochrome","documentary","moody","grain","portrait"]""",
            source         = "BUILT_IN"
        ),

        RecipeEntity(
            name           = "Golden Hour Glow",
            description    = "Warm, slightly desaturated tones that enhance the soft light of sunrise and sunset.",
            baseSimulation = "Classic Chrome",
            settingsJson   = """{"film_simulation":"Classic Chrome","color":"-1","highlight_tone":"-2","shadow_tone":"1","wb_shift_red":"2","wb_shift_blue":"-3","grain_effect":"Weak Small","color_chrome_effect":"Weak","dynamic_range":"DR200"}""",
            tagsJson       = """["landscape","golden hour","sunset","sunrise","warm","muted"]""",
            source         = "BUILT_IN"
        ),

        RecipeEntity(
            name           = "Cinematic Teal-Orange",
            description    = "Modern film look with split toning — warm highlights, cool shadows.",
            baseSimulation = "Eterna/Cinema",
            settingsJson   = """{"film_simulation":"Eterna/Cinema","color":"-2","highlight_tone":"-1","shadow_tone":"-1","wb_shift_red":"2","wb_shift_blue":"-1","clarity":"-1","grain_effect":"Weak Large","color_chrome_effect":"Strong","d_range_priority":"Weak"}""",
            tagsJson       = """["cinematic","portrait","street","split-tone"]""",
            source         = "BUILT_IN"
        ),

        RecipeEntity(
            name           = "Bright & Airy",
            description    = "High-key, soft and clean. Ideal for lifestyle, product, and food photography in natural light.",
            baseSimulation = "Astia/Soft",
            settingsJson   = """{"film_simulation":"Astia/Soft","highlight_tone":"2","shadow_tone":"2","color":"1","sharpness":"-1","noise_reduction":"1","wb_shift_red":"1","wb_shift_blue":"-1","clarity":"-1"}""",
            tagsJson       = """["lifestyle","food","product","clean","airy","light"]""",
            source         = "BUILT_IN"
        ),

        RecipeEntity(
            name           = "Nostalgic Expired Film",
            description    = "Faded, slightly green-shifted tones that mimic the look of expired consumer film from the 90s.",
            baseSimulation = "Nostalgic Neg.",
            settingsJson   = """{"film_simulation":"Nostalgic Neg.","color":"-2","highlight_tone":"2","shadow_tone":"2","wb_shift_red":"-1","wb_shift_blue":"2","grain_effect":"Strong Small","sharpness":"-2","noise_reduction":"-1"}""",
            tagsJson       = """["nostalgic","expired","faded","vintage","street","casual"]""",
            source         = "BUILT_IN"
        ),

        RecipeEntity(
            name           = "Bleach Bypass Drama",
            description    = "Desaturated, high-contrast look reminiscent of silver retention film processing.",
            baseSimulation = "Eterna Bleach Bypass",
            settingsJson   = """{"film_simulation":"Eterna Bleach Bypass","color":"-4","highlight_tone":"1","shadow_tone":"-2","sharpness":"2","clarity":"3","grain_effect":"Weak Large","noise_reduction":"-2"}""",
            tagsJson       = """["dramatic","desaturated","high-contrast","cinematic","architecture"]""",
            source         = "BUILT_IN"
        )
    )
}
```

### 9.4 Recipe Matching Logic

When Claude returns `suggested_settings`, the AI repository also performs a secondary match: it compares the suggested settings map against all built-in recipes using a weighted cosine-similarity-style overlap score. The top-2 matching built-in recipes are appended to `AISuggestion.suggestedRecipes` alongside the newly generated recipe.

```kotlin
fun findClosestRecipes(
    suggestedSettings: Map<String, String>,
    allRecipes: List<FilmSimulationRecipe>,
    topN: Int = 2
): List<FilmSimulationRecipe> {
    return allRecipes
        .filter { it.source == RecipeSource.BUILT_IN }
        .map { recipe ->
            val overlapKeys = recipe.settings.keys.intersect(suggestedSettings.keys)
            val matchScore = overlapKeys.count { key ->
                recipe.settings[key] == suggestedSettings[key]
            }.toFloat() / maxOf(recipe.settings.size, suggestedSettings.size)
            recipe to matchScore
        }
        .sortedByDescending { (_, score) -> score }
        .take(topN)
        .map { (recipe, _) -> recipe }
}
```

---

## 10. Implementation Phases

### Sprint 1 — Foundation & Camera (Weeks 1–3)

**Goal**: A running app with live camera preview and camera model selection.

| Task | Details |
|---|---|
| Dependency setup | Add all libs to `libs.versions.toml` and `app/build.gradle.kts` |
| Hilt application class | Create `PhotofriendApp : Application()`, annotate with `@HiltAndroidApp`, register in manifest |
| Room schema | Define entities, DAOs, and `PhotofriendDatabase`; write migration strategy comment |
| Seed data | `CameraSeeds.kt` with X-T30 III, X-T5, X100VI, X-S20 full settings |
| Navigation scaffold | `NavGraph.kt` with all routes; `Screen.kt` sealed class |
| CameraSelectScreen | List of camera models from Room via `GetCameraModelsUseCase` |
| CameraX integration | `CameraManager`, `FrameAnalyzer`, `CameraPreview` composable |
| ViewfinderScreen | Full-screen `CameraPreview` + permission handling + bottom nav bar |
| Unit tests | `GetCameraModelsUseCase`, `GetCameraSettingsUseCase`, mapper functions |

**Deliverable**: User can select a camera model and see a live camera preview.

---

### Sprint 2 — Settings UI & AI Integration (Weeks 4–6)

**Goal**: Full settings display and working Claude API analysis.

| Task | Details |
|---|---|
| CameraSettingsScreen | Grouped settings list with `SettingSlider` and `SettingChip` components; state persisted in DataStore |
| Network layer | `ClaudeApiService` via Retrofit; `OkHttpClient` with logging interceptor (debug only) |
| AI repository | `AIRepositoryImpl.analyzeScene()` with prompt construction and JSON parsing |
| `AnalyzeSceneUseCase` | Orchestrates bitmap capture → API call → parse → return `AISuggestion` |
| AISuggestionScreen | Displays scene description, settings diff (highlighting what changed), and recipe cards |
| Recipe matching | `findClosestRecipes()` integrated into `AIRepositoryImpl` |
| Error states | Network error, API quota, malformed JSON — all surfaced as distinct `UiState` error variants |
| Loading indicators | Full-screen shimmer while awaiting Claude response |
| Integration tests | `AIRepositoryImpl` with a mock Retrofit using a canned JSON fixture |

**Deliverable**: User can tap "Analyze", see Claude's settings recommendation overlaid on the viewfinder result screen.

---

### Sprint 3 — Recipes, Polish & Release Prep (Weeks 7–9)

**Goal**: Complete recipe engine, save/browse functionality, UI polish, release readiness.

| Task | Details |
|---|---|
| Seed recipes | All 7+ built-in `RecipeSeeds` with complete settings maps |
| SavedRecipesScreen | Grid layout with `RecipeCard`, filter chips by tag, long-press delete |
| RecipeDetailScreen | Full settings breakdown, share intent (plain text), "Apply to camera" deep-link |
| Save AI recipes | From `AISuggestionScreen`, user can save the AI-generated recipe to Room |
| Settings persistence | DataStore stores the user's active settings per camera model, restored on app relaunch |
| Theme refinement | Replace default purple palette with a photography-appropriate warm/neutral palette |
| ProGuard rules | Write rules for Retrofit, Gson, Room, Hilt once `isMinifyEnabled` is toggled on |
| Manifest permissions | Final camera permission handling with a rationale dialog for denied cases |
| API key security | Document `local.properties` workflow; add `BuildConfig.DEBUG` guard on logging interceptor |
| Performance | Profile with Android Studio profiler; optimize `FrameAnalyzer` throttle; defer heavy DB seed to `WorkManager` one-shot job if needed |
| Instrumented tests | `CameraSelectScreen`, `ViewfinderScreen` basic UI tests with Compose testing APIs |

**Deliverable**: Feature-complete app ready for internal alpha distribution (`.aab` via `bundleRelease`).

---

## Appendix A — Fujifilm Film Simulations Reference

| Simulation | Character | Best For |
|---|---|---|
| Provia/Standard | Neutral, accurate | General purpose, portraits |
| Velvia/Vivid | High saturation, punchy | Landscapes, nature |
| Astia/Soft | Soft, pastel tones | Portraits, fashion |
| Classic Chrome | Muted, cinematic | Street, documentary |
| Reala Ace | Natural, subtle | Everyday, travel |
| Pro Neg. Hi | Medium contrast | Portraits, studio |
| Pro Neg. Std | Low contrast, smooth | Studio, skin tones |
| Classic Neg. | Faded, filmic | Street, nostalgia |
| Nostalgic Neg. | Warm, slightly faded | Casual, lifestyle |
| Eterna/Cinema | Flat, cinema log-like | Video, post-processing |
| Eterna Bleach Bypass | Desaturated, harsh | Drama, architecture |
| Acros | Rich BW with grain | Black and white |
| Monochrome | Clean BW | Black and white |
| Sepia | Warm brown tone | Vintage, artistic |

---

## Appendix B — Key Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| DI framework | Hilt | Official Google recommendation; integrates with `@HiltViewModel`; compile-time verification |
| Local DB | Room | Type-safe SQLite; Flow-based reactivity; official Jetpack |
| HTTP client | Retrofit + OkHttp | Industry standard; interceptor support for auth headers; easy to mock in tests |
| Image loading | Coil | Kotlin-first; Compose integration (`AsyncImage`); small footprint |
| Async | Coroutines + Flow | Native Kotlin; integrates with Room, Retrofit (`suspend`), and Compose (`collectAsState`) |
| AI transport | REST API | No official Anthropic Android SDK; REST is stable and version-controlled via `anthropic-version` header |
| Settings storage | DataStore Preferences | Replaces SharedPreferences; coroutine-native; no ANRs |
| Navigation | Navigation Compose | Single-activity; type-safe args; integrates with Hilt via `hiltViewModel()` |
