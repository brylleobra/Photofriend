# Changelog

All notable changes to Photofriend are documented here.

Format: `v[major].[sprint].[hotfix]`
- **major** — significant product milestone
- **sprint** — completed sprint increment
- **hotfix** — patch on top of a sprint release

---

## [v0.4.0] — 2026-03-06

### Added
- **Tap to focus** — tap anywhere on the live preview to set the AF/AE metering point via CameraX `FocusMeteringAction`; focus lock auto-cancels after 3 seconds
- **Animated focus ring** — a yellow square indicator (matching Fujifilm OVF style) appears at the tap position, animates from 140 % → 100 % scale, then fades out after ~1 s
- **Aperture simulation** — new `Aperture` setting on X-T30 III with options `Native`, `f/1.0` through `f/16`:
  - **Exposure compensation** (real hardware effect) — maps the chosen f-stop to an EV offset relative to the phone's native aperture (~f/2.0) and applies it via `cameraControl.setExposureCompensationIndex()`
  - **Vignette overlay** (simulated) — radial gradient darkens corners; intensity scales with aperture width (f/1.0 = strongest, f/5.6+ = minimal)
  - Setting description in the UI is explicit that depth-of-field / bokeh cannot be simulated without depth map hardware
- **Vignette overlay composable** (`VignetteOverlay`) — reusable radial gradient Canvas layer in `CameraPreview`; driven by `ViewfinderEffectParams.vignetteStrength`
- `ViewfinderEffectParams` — two new fields: `vignetteStrength: Float` and `exposureEvOffset: Int`
- `CameraManager.focusAt()` and `CameraManager.setExposureOffset()` — new public methods
- `FilmLook.apertureParams()` — maps f-stop string to `(vignetteStrength, evOffset)` pair

### Fixed
- **Portrait photos saved rotated 90°** — `CameraManager.captureFrame()` now reads `ImageProxy.imageInfo.rotationDegrees` and applies a `Matrix.postRotate()` correction before returning the bitmap; landscape capture (rotation = 0°) is unaffected
- **Unit tests failing to compile** — `CameraSettingsViewModelTest` was written against the old single-argument `CameraSettingsViewModel(useCase)` constructor and the removed `getSettingsFlow()` method; fully rewritten to match the current API (`init(cameraId)` + `uiState` StateFlow + mocked `SettingsStore`)

### Changed
- **Aperture setting added to X-T30 III seed data** (`CameraSeeds.xt30iiiSettings`) — 14 settings total
- **DB seeder made additive** — `seedSettingsIfEmpty` and `seedCamerasIfEmpty` in `CameraRepositoryImpl` now call `insertAll` unconditionally; `OnConflictStrategy.IGNORE` on both DAOs ensures existing rows are skipped and only new rows (e.g. Aperture) are inserted on existing installs
- Room database bumped to **version 4**; `fallbackToDestructiveMigration()` triggers full re-seed on upgrade
- App `versionCode` → 4, `versionName` → `0.4.0`

---

## [v0.3.0] — 2026-03-06

### Added
- **Apply button for camera settings** — changes are now staged locally in `_pendingChanges: MutableStateFlow` and only written to DataStore when the user taps Apply; the button appears in a bottom bar only when there are uncommitted changes
- **Live viewfinder effects** — camera settings are now visually injected into the preview without AI:
  - *Tier 1 — Camera2 hardware*: Noise Reduction and Sharpness map to `CaptureRequest.NOISE_REDUCTION_MODE` and `CaptureRequest.EDGE_MODE` via Camera2 interop
  - *Tier 2 — GPU colour matrix*: Film Simulation, White Balance, Highlight/Shadow Tone, Color saturation, Color Chrome Effect, Color Chrome FX Blue, and Clarity are composed into a single `android.graphics.ColorMatrix` and applied to the `PreviewView` via `View.setLayerType(LAYER_TYPE_HARDWARE, paint)` with `ColorMatrixColorFilter`
  - Animated film grain overlay (`GrainOverlay`) renders at ~12 fps using Compose `Canvas` with `BlendMode.Overlay`
- **Film simulation look definitions** (`FilmLook.kt`) — colour matrix recipes for all 20 Fujifilm film simulations: Provia/Standard, Velvia/Vivid, Astia/Soft, Classic Chrome, Pro Neg. Hi, Pro Neg. Std, Classic Neg., Nostalgic Neg., Eterna/Cinema, Eterna Bleach Bypass, Acros (+ filter variants), Monochrome (+ filter variants), Sepia, Reala Ace
- **Photo capture with film simulation** — shutter button in the viewfinder captures a frame, applies the current colour matrix via `BitmapUtils.applyColorMatrix()`, and saves the result as a JPEG in `DCIM/Photofriend` using MediaStore (API 29+) or legacy `FileOutputStream` + `MediaScannerConnection` (API < 29); success/failure shown via snackbar
- **Apply AI suggestions to camera settings** — "Apply to Camera Settings" button on the AI Suggestion screen writes all suggested setting values back to DataStore by resolving setting names → IDs via `GetCameraSettingsUseCase`; viewfinder effects update immediately after
- `ViewfinderEffectParams` data class — carries `colorMatrixValues`, `grainAmount`, `grainSizePx` for the preview layer
- `BitmapUtils.applyColorMatrix()` — renders a bitmap through a 4×5 colour matrix on a hardware canvas
- `BitmapUtils.saveToGallery()` — saves a JPEG to the device gallery with correct scoped storage handling

### Fixed
- **Settings changes not reflected in viewfinder** — root cause was `CameraSettingsViewModel` returning a new Flow per call; redesigned with a stable `uiState: StateFlow` driven by `_cameraId.flatMapLatest { combine(...) }`
- **Color / Highlight / Shadow / Clarity had imperceptible effect** — multipliers were too small; increased: Color step `0.08 → 0.12`, tone brightness step `4/2 → 8/5`, tone contrast step `0.022 → 0.03`, Clarity step `0.014 → 0.025`
- **Gemini API 403 / API key not loading** — `project.findProperty()` does not read `local.properties`; replaced with an explicit `Properties()` loader in `build.gradle.kts`
- **`ExceptionInInitializerError` on app start** — `ViewfinderEffectParams.NONE` referenced `IDENTITY` before it was initialised in the companion object; fixed by declaring `IDENTITY` before `NONE`
- **`Unresolved reference 'colorFilter'`** — `GraphicsLayerScope.colorFilter` requires Compose UI 1.8+; BOM `2024.09.00` ships 1.7.0; workaround: apply colour matrix via `View.setLayerType(LAYER_TYPE_HARDWARE, paint)` in the `AndroidView` update lambda instead
- **APK load failure** (`Failed to load asset path`) — caused by a corrupt stale APK on device; resolved by `./gradlew clean assembleDebug` followed by uninstall/reinstall

### Changed
- **AI provider migrated from Gemini to Ollama Cloud** — model `qwen3-vl:235b-cloud` at `https://ollama.com/api/chat`; request format updated to Ollama native `messages` array with `system` / `user` roles and `images` as a base64 list; optional Bearer auth interceptor reads `ollama.api.key` from `local.properties`; read timeout extended to 120 s for large model latency
- `CameraSettingsViewModel` — redesigned with `_pendingChanges` buffer; `onSettingChanged` no longer writes to DataStore immediately; `applyChanges()` commits all pending changes atomically
- `WRITE_EXTERNAL_STORAGE` permission added to manifest with `maxSdkVersion="28"` (not required on API 29+)
- Room database bumped to **version 3**
- App `versionCode` → 3, `versionName` → `0.3.0`

---

## [v0.2.0] — 2026-03-06

### Added
- **15 built-in film simulation recipes** (expanded from 5), organised across 8 shooting categories:
  - *Colour / Slide*: Vintage Kodachrome, Sharp Landscape
  - *Documentary / Street*: Classic Chrome Street, Urban Grit
  - *Golden Hour / Warm*: Golden Hour Glow, Warm Indoor
  - *Portrait*: Soft Portrait, Studio Portrait, Airy Summer
  - *Cinematic / Mood*: Cinematic Night, Moody Overcast
  - *Black & White*: Moody Monochrome, Film Noir
  - *Vintage / Film*: Nostalgic Print
  - *Natural / Travel*: Travel Diary
- **Clarity parameter** added end-to-end: `FilmSimulationRecipe`, `RecipeEntity`, mapper, Recipe Detail screen, share text, and all 15 seed recipes
- **WB Shift R / WB Shift B** fields now requested from AI in the prompt JSON schema and correctly saved when the user saves an AI-generated recipe
- Clarity row visible in Recipe Detail parameter table and included in the plain-text share output

### Fixed
- `buildRecipeFromSuggestion` was silently dropping positive tone/color/sharpness values — Kotlin's `toIntOrNull()` returns `null` for strings like `"+1"`; replaced with a `toSignedInt()` helper that strips the leading `+` before parsing
- WB Shift R and WB Shift B were hardcoded to `0` when saving AI recipes; both are now parsed from the AI response
- Clarity was never included in saved AI recipes; now mapped correctly

### Changed
- Room database bumped to **version 2**; `fallbackToDestructiveMigration()` added (pre-release builds only)
- App `versionCode` → 2, `versionName` → `0.2.0`

---

## [v0.1.0] — 2026-03-06

### Sprint 3 — Recipes, Polish & Release Prep

#### Added
- **SettingsStore** (`di/SettingsStore.kt`) — DataStore-backed singleton that persists user's selected camera settings (per camera model) across navigation and app restarts
- **DataStoreModule** (`di/DataStoreModule.kt`) — Hilt module providing a single `DataStore<Preferences>` instance
- **SelectedRecipeStore** (`di/SelectedRecipeStore.kt`) — in-memory singleton used to pass the tapped recipe from `RecipesScreen` into `RecipeDetailScreen` without complex nav-arg serialisation
- **DeleteRecipeUseCase** — clean-architecture use case wrapping `CameraRepository.deleteRecipe()`
- **RecipeDetailScreen + RecipeDetailViewModel** (`ui/screen/recipedetail/`) — full parameter breakdown for all 12 settings, with an Android share-sheet button that exports the recipe as formatted plain text
- **RecipesScreen tabs** — "Built-in" and "My Recipes" tabs using `TabRow`; My Recipes tab shows the saved count in its label
- **Swipe-to-delete on saved recipes** — `SwipeToDismissBox` with a red delete icon revealed on swipe; fires a Snackbar with "Undo" that re-inserts the recipe if tapped within 4 seconds
- **Tap-to-detail on any recipe** — both Built-in and My Recipes cards navigate to `RecipeDetailScreen`
- **Photography colour theme** — replaced default Material 3 purple palette with a warm earthy palette: Cognac (primary), Warm Grey (secondary), Forest Green (tertiary); applied to both light and dark schemes

#### Fixed
- **Settings not persisting across navigation** — `CameraSettingsViewModel` previously stored selections in a `MutableStateFlow` that was destroyed on navigation; now reads from and writes to `SettingsStore` (DataStore) on every change
- **AI ignoring user setting preferences** — `AIRepositoryImpl.analyzeScene()` previously fetched raw DB defaults and never read the user's selections; now calls `settingsStore.getValuesSnapshot(cameraId)` and injects the user's current values into the AI system prompt with an explicit instruction to respect them
- **HTTP logging in release builds** — `HttpLoggingInterceptor` level was unconditionally set to `BODY`; now guarded by `BuildConfig.DEBUG`

#### Changed
- `RecipesScreen` / `RecipesViewModel` refactored to support two tabs, delete with undo, and recipe selection; `RecipesUiState` redesigned with `builtInRecipes`, `savedRecipes`, and `activeTab` fields
- `Screen.kt` — added `RecipeDetail` route
- `NavGraph.kt` — wired `RecipesScreen.onRecipeClick` → `RecipeDetail`, added `RecipeDetailScreen` composable

---

### Sprint 2 — Settings UI & AI Integration (built prior to v0.1.0 tag)

#### Added
- **CameraSettingsScreen + CameraSettingsViewModel** (`ui/screen/settings/`) — scrollable, grouped list of all camera settings with `ExposedDropdownMenuBox` selectors; organised by category (Color, Exposure, Output); Reset button restores defaults
- **AI scene analysis** — full AI pipeline:
  - `GeminiApiService` (Retrofit interface, `POST v1beta/models/gemini-2.0-flash:generateContent`)
  - `GeminiRequestDto` / `GeminiResponseDto` DTOs with system instruction, inline image data, and `responseMimeType: "application/json"`
  - `AIRepositoryImpl` — captures frame → base64-encodes at ≤1024px → builds system prompt with camera model + valid setting options → parses JSON response → graceful fallback on malformed JSON
  - `AnalyzeSceneUseCase`
- **AISuggestionScreen + AISuggestionViewModel** (`ui/screen/aisuggestion/`) — displays scene description, full settings table, recipe name, and AI reasoning; bookmark button saves to Room
- **AISuggestionStore** (`di/AISuggestionStore.kt`) — singleton passing the `AISuggestion` from `ViewfinderViewModel` to `AISuggestionViewModel` without navigation arguments
- **SaveRecipeUseCase**
- `GetFilmSimulationRecipesUseCase` — exposes `builtIn()` and `saved()` flows
- Loading spinner + "Analyzing scene…" text shown during AI API call
- Error snackbar surfaces network and API errors from the viewfinder

#### Changed
- AI provider initially set to **Gemini API** (`gemini-2.0-flash`):
  - Base URL: `https://generativelanguage.googleapis.com/`
  - Authentication: `?key=` query parameter
  - `BuildConfig` field: `GEMINI_API_KEY`; set via `gemini.api.key` in `local.properties`

---

### Sprint 1 — Foundation & Camera (built prior to v0.1.0 tag)

#### Added
- **Single-activity Compose app** with `@HiltAndroidApp` application class (`PhotofriendApp`)
- **Navigation scaffold** — `NavGraph.kt` + `Screen.kt` sealed class; routes: CameraSelect → Viewfinder → CameraSettings → AISuggestion; CameraSelect → Recipes → RecipeDetail
- **CameraSelectScreen + CameraSelectViewModel** — searchable list of Fujifilm camera models loaded from Room via `GetCameraModelsUseCase`
- **ViewfinderScreen + ViewfinderViewModel** — full-screen CameraX live preview; camera permission request/rationale/denial handling; "Analyze Scene" and "Settings" action buttons; `CameraManager` singleton (`ImageCapture`-based frame capture); `CameraPreview` composable wrapping `AndroidView`
- **Room database** (`PhotofriendDatabase`, version 1) with three entities:
  - `CameraModelEntity` — brand, name, sensor size, megapixels, film simulation count, release year
  - `CameraSettingEntity` — per-camera setting with name, description, category, pipe-delimited options, default value
  - `RecipeEntity` — full recipe parameter set including `isBuiltIn` / `isUserSaved` flags
- **Seed data**:
  - 5 Fujifilm camera models: X-T30 III, X-T5, X-S20, X100VI, X-Pro3
  - Full settings for X-T30 III (13 settings across Color, Exposure, Output categories)
  - 5 built-in film simulation recipes
- **Domain layer** — `CameraModel`, `CameraSetting`, `FilmSimulationRecipe`, `AISuggestion` models; `CameraRepository` + `AIRepository` interfaces; use cases: `GetCameraModelsUseCase`, `GetCameraSettingsUseCase`, `GetFilmSimulationRecipesUseCase`
- **Hilt DI** — `DatabaseModule`, `NetworkModule`, `RepositoryModule`; `@HiltViewModel` on all ViewModels
- **Material 3 theme** with dynamic colour on Android 12+ and warm photography static palette fallback
- **Manifest** — `CAMERA` and `INTERNET` permissions; `android.hardware.camera` feature declared as not required
- Unit tests: `GetCameraModelsUseCaseTest`, `GetCameraSettingsUseCaseTest`, `GetFilmSimulationRecipesUseCaseTest`, `CameraSelectViewModelTest`, `CameraSettingsViewModelTest`, `CameraMapperTest`

---

## Known Issues (open as of v0.4.0)

| # | Description | Impact |
|---|---|---|
| 1 | Settings only seeded for X-T30 III — `CameraRepositoryImpl.seedSettingsIfEmpty` hardcodes the camera ID | Selecting X-T5, X-S20, X100VI, or X-Pro3 leaves the Settings screen empty and provides no setting context to the AI prompt |
| 2 | `cameraModel` race condition in `ViewfinderViewModel` — `_cameraId` read as a snapshot value rather than combined as a flow | Can result in "Camera model not loaded yet" error if Analyze is tapped immediately after navigation |
| 3 | Aperture simulation does not include depth-of-field / bokeh | True subject separation requires per-pixel depth data; not available on single-camera phones without Portrait Mode APIs |
