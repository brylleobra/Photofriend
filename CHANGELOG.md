# Changelog

All notable changes to Photofriend are documented here.

Format: `v[major].[sprint].[hotfix]`
- **major** — significant product milestone
- **sprint** — completed sprint increment
- **hotfix** — patch on top of a sprint release

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
- **WB Shift R / WB Shift B** fields now requested from Gemini in the AI prompt JSON schema and correctly saved when the user saves an AI-generated recipe
- Clarity row visible in Recipe Detail parameter table and included in the plain-text share output

### Fixed
- `buildRecipeFromSuggestion` was silently dropping positive tone/color/sharpness values — Kotlin's `toIntOrNull()` returns `null` for strings like `"+1"`. Replaced with a `toSignedInt()` helper that strips the leading `+` before parsing
- WB Shift R and WB Shift B were hardcoded to `0` when saving AI recipes; both are now parsed from the Gemini response
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
- **Settings not persisting across navigation** — `CameraSettingsViewModel` previously stored selections in a `MutableStateFlow` that was destroyed on navigation. Now reads from and writes to `SettingsStore` (DataStore) on every change
- **AI ignoring user setting preferences** — `AIRepositoryImpl.analyzeScene()` previously fetched raw DB defaults and never read the user's selections. Now calls `settingsStore.getValuesSnapshot(cameraId)` and injects the user's current values into the Gemini system prompt with an explicit instruction to respect them
- **HTTP logging in release builds** — `HttpLoggingInterceptor` level was unconditionally set to `BODY`; now guarded by `BuildConfig.DEBUG`

#### Changed
- `RecipesScreen` / `RecipesViewModel` refactored to support two tabs, delete with undo, and recipe selection; `RecipesUiState` redesigned with `builtInRecipes`, `savedRecipes`, and `activeTab` fields
- `Screen.kt` — added `RecipeDetail` route
- `NavGraph.kt` — wired `RecipesScreen.onRecipeClick` → `RecipeDetail`, added `RecipeDetailScreen` composable

---

### Sprint 2 — Settings UI & AI Integration (built prior to v0.1.0 tag)

#### Added
- **CameraSettingsScreen + CameraSettingsViewModel** (`ui/screen/settings/`) — scrollable, grouped list of all camera settings with `ExposedDropdownMenuBox` selectors; organised by category (Color, Exposure, Output); Reset button restores defaults
- **AI scene analysis** — full Claude → Gemini pipeline:
  - `GeminiApiService` (Retrofit interface, `POST v1beta/models/gemini-2.0-flash:generateContent`)
  - `GeminiRequestDto` / `GeminiResponseDto` DTOs with system instruction, inline image data, and `responseMimeType: "application/json"`
  - `AIRepositoryImpl` — captures frame → base64-encodes at ≤1024px → builds system prompt with camera model + valid setting options → parses JSON response → graceful fallback on malformed JSON
  - `AnalyzeSceneUseCase`
- **AISuggestionScreen + AISuggestionViewModel** (`ui/screen/aisuggestion/`) — displays scene description, full settings table, recipe name, and AI reasoning; bookmark button saves to Room
- **AISuggestionStore** (`di/AISuggestionStore.kt`) — singleton passing the `AISuggestion` from `ViewfinderViewModel` to `AISuggestionViewModel` without navigation arguments
- **SaveRecipeUseCase**
- `GetFilmSimulationRecipesUseCase` — exposes `builtIn()` and `saved()` flows
- Loading spinner + "Analyzing scene…" text shown during Gemini API call
- Error snackbar surfaces network and API errors from the viewfinder

#### Changed
- AI provider switched from **Claude API** to **Gemini API** (`gemini-2.0-flash`):
  - Base URL changed to `https://generativelanguage.googleapis.com/`
  - Authentication changed from `x-api-key` header to `?key=` query parameter
  - `BuildConfig` field renamed `CLAUDE_API_KEY` → `GEMINI_API_KEY`; set via `gemini.api.key` in `local.properties`

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

## Known Issues (open as of v0.2.0)

| # | Description | Impact |
|---|---|---|
| 1 | Settings only seeded for X-T30 III — `CameraRepositoryImpl.seedSettingsIfEmpty` hardcodes the camera ID | Selecting X-T5, X-S20, X100VI, or X-Pro3 leaves Settings screen in loading state and AI prompt has no setting context |
| 2 | `getSettingsFlow(cameraId)` called without `remember {}` in `CameraSettingsScreen` | New DataStore subscription created on every recomposition; minor performance impact and potential flicker |
| 3 | `cameraModel` race condition in `ViewfinderViewModel` — `_cameraId` read as a snapshot value rather than combined as a flow | Can result in "Camera model not loaded yet" error when Analyze is tapped immediately after navigation |
