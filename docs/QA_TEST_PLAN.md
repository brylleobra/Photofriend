# Photofriend — QA Test Plan

**Document version**: 1.0
**Date**: 2026-03-05
**Author**: QA Engineering
**App version target**: 1.0 (initial feature-complete release)
**Platform**: Android (min SDK 24 / Android 7.0, target SDK 36 / Android 15)
**Package**: `com.example.photofriend`

---

## Table of Contents

1. [Testing Strategy Overview](#1-testing-strategy-overview)
2. [Unit Test Plan](#2-unit-test-plan)
3. [Integration Test Plan](#3-integration-test-plan)
4. [Instrumented / UI Test Plan](#4-instrumented--ui-test-plan)
5. [Manual Test Cases](#5-manual-test-cases)
6. [Edge Cases and Risk Areas](#6-edge-cases-and-risk-areas)
7. [Test Data](#7-test-data)
8. [Definition of Done](#8-definition-of-done)
9. [CI/CD Recommendations](#9-cicd-recommendations)

---

## 1. Testing Strategy Overview

### 1.1 Guiding Principles

Photofriend combines hardware access (camera), network calls (Claude AI API), and persistent state (Room database). These three concerns have very different failure modes and test requirements. The strategy is layered:

- **Maximize fast feedback**: The bulk of correctness verification happens in JVM unit tests (no device, run in seconds).
- **Verify integration boundaries**: Room DAOs, Retrofit clients, and Repository implementations are tested with real in-memory databases and mock servers, not hand-rolled fakes, to catch contract mismatches early.
- **Validate user journeys with Compose UI tests**: Automated instrumented tests cover the happy paths of every user-facing screen using a device or emulator.
- **Manual testing owns hardware and AI quality**: CameraX lifecycle, actual AI response quality, and physical device ergonomics cannot be fully automated.

### 1.2 Test Pyramid

```
              /\
             /  \   Manual Tests
            /----\  (CameraX hardware, AI quality)
           /      \
          / Instrum.\  UI / Instrumented Tests
         /  (Compose)\  (screen flows, navigation)
        /-------------\
       /  Integration  \  (Room DB, Retrofit, Repo)
      /-----------------\
     /    Unit Tests     \  (ViewModels, UseCases, Parsers, Logic)
    /---------------------\
```

| Layer | Tooling | Speed | Device Required |
|---|---|---|---|
| Unit | JUnit 4, Mockito/MockK, Turbine, kotlinx-coroutines-test | ~seconds | No |
| Integration | JUnit 4, Room in-memory, MockWebServer (OkHttp) | ~seconds–minutes | No |
| UI / Instrumented | Compose UI Test, AndroidX Test, Espresso | ~minutes | Yes |
| Manual | Human tester, physical device | ~hours | Yes |

### 1.3 Scope Summary

| Feature Area | Unit | Integration | UI | Manual |
|---|---|---|---|---|
| Camera permission flow | - | - | Yes | Yes |
| CameraX viewfinder | - | - | Smoke | Yes |
| Camera model selection | Yes | Yes | Yes | Yes |
| Settings display and filtering | Yes | - | Yes | Yes |
| AI scene analysis (Claude API) | Yes (parser) | Yes (API client) | Yes | Yes |
| Film simulation recipes | Yes | Yes (Room) | Yes | Yes |
| Recipe save / favourites | Yes | Yes (Room) | Yes | Yes |
| Navigation | - | - | Yes | Yes |
| Offline / error states | Yes | Yes | Yes | Yes |

---

## 2. Unit Test Plan

All tests in this section run on the JVM with `./gradlew test`. No Android framework is needed. Use **MockK** for Kotlin-idiomatic mocking and **kotlinx-coroutines-test** (`runTest`, `TestDispatcher`) for coroutine-based code. Use **Turbine** for testing `StateFlow` / `SharedFlow` emissions.

### 2.1 Dependencies to Add (libs.versions.toml)

```toml
[versions]
mockk              = "1.13.10"
turbine            = "1.1.0"
coroutinesTest     = "1.8.0"
truth              = "1.4.2"

[libraries]
mockk              = { group = "io.mockk",                name = "mockk",                     version.ref = "mockk" }
turbine            = { group = "app.cash.turbine",        name = "turbine",                   version.ref = "turbine" }
coroutines-test    = { group = "org.jetbrains.kotlinx",  name = "kotlinx-coroutines-test",   version.ref = "coroutinesTest" }
google-truth       = { group = "com.google.truth",        name = "truth",                     version.ref = "truth" }
```

---

### 2.2 ViewModel Tests

#### 2.2.1 CameraSelectionViewModel

**File**: `app/src/test/java/com/example/photofriend/ui/cameraselection/CameraSelectionViewModelTest.kt`

```kotlin
class CameraSelectionViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val getCameraModelsUseCase: GetCameraModelsUseCase = mockk()
    private lateinit var viewModel: CameraSelectionViewModel

    @Before fun setUp() {
        every { getCameraModelsUseCase() } returns flowOf(TestData.allCameraModels)
        viewModel = CameraSelectionViewModel(getCameraModelsUseCase)
    }

    @Test fun `initial state shows loading then camera list`() = runTest {
        viewModel.uiState.test {
            val loading = awaitItem()
            assertThat(loading.isLoading).isTrue()
            val loaded = awaitItem()
            assertThat(loaded.cameras).hasSize(TestData.allCameraModels.size)
            assertThat(loaded.isLoading).isFalse()
        }
    }

    @Test fun `search query filters cameras by name`() = runTest {
        viewModel.uiState.test {
            awaitItem() // loading
            awaitItem() // loaded
            viewModel.onSearchQueryChanged("X-T30")
            val filtered = awaitItem()
            assertThat(filtered.cameras).allMatch { it.name.contains("X-T30", ignoreCase = true) }
        }
    }

    @Test fun `search query empty restores full list`() = runTest {
        viewModel.onSearchQueryChanged("X-T30")
        viewModel.onSearchQueryChanged("")
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.cameras).hasSize(TestData.allCameraModels.size)
        }
    }

    @Test fun `selecting a camera emits navigation event`() = runTest {
        viewModel.navigationEvent.test {
            viewModel.onCameraSelected(TestData.xtFujifilm)
            val event = awaitItem()
            assertThat(event).isInstanceOf(CameraSelectionNavigationEvent.NavigateToSettings::class.java)
            assertThat((event as CameraSelectionNavigationEvent.NavigateToSettings).cameraId)
                .isEqualTo(TestData.xtFujifilm.id)
        }
    }

    @Test fun `use case error surfaces in ui state`() = runTest {
        every { getCameraModelsUseCase() } returns flow { throw IOException("network") }
        viewModel = CameraSelectionViewModel(getCameraModelsUseCase)
        viewModel.uiState.test {
            awaitItem() // loading
            val errorState = awaitItem()
            assertThat(errorState.error).isNotNull()
        }
    }
}
```

#### 2.2.2 CameraSettingsViewModel

**File**: `app/src/test/java/com/example/photofriend/ui/settings/CameraSettingsViewModelTest.kt`

| Test ID | Test Name | Input | Expected Outcome |
|---|---|---|---|
| VM-S-01 | Settings load for selected camera | cameraId = "xt30iii" | `uiState.settings` list is non-empty |
| VM-S-02 | Filter by category "Exposure" | `onCategorySelected("Exposure")` | Only exposure-related settings shown |
| VM-S-03 | Filter by category "All" | `onCategorySelected("All")` | All settings for camera shown |
| VM-S-04 | AI button triggers analysis | `onAnalyzeSceneClicked()` | `uiState.isAnalyzing == true` while API pending |
| VM-S-05 | AI response populates suggestions | Mocked API returns valid JSON | `uiState.aiSuggestions` matches parsed result |
| VM-S-06 | Camera with no settings shows empty state | cameraId = "unknown" | `uiState.isEmpty == true` |

#### 2.2.3 AiSuggestionViewModel

| Test ID | Test Name | Input | Expected Outcome |
|---|---|---|---|
| VM-AI-01 | Initial state is idle | ViewModel created | `uiState.status == Idle` |
| VM-AI-02 | Loading state during API call | `analyzeScene()` called | `uiState.status == Loading` |
| VM-AI-03 | Success state after API returns | Mock returns valid response | `uiState.status == Success`, suggestions non-empty |
| VM-AI-04 | Error state on API failure | Mock throws `HttpException(503)` | `uiState.status == Error` with user-friendly message |
| VM-AI-05 | Retry after error resets to loading | `retry()` called after error | `uiState.status` transitions Loading -> Success/Error |
| VM-AI-06 | API timeout surfaces as error | Mock delays > timeout threshold | `uiState.status == Error` |

#### 2.2.4 FilmSimulationViewModel

| Test ID | Test Name | Input | Expected Outcome |
|---|---|---|---|
| VM-FS-01 | All recipes load on init | Repository returns 20 recipes | `uiState.recipes.size == 20` |
| VM-FS-02 | AI-suggested recipes appear first | AI returns 3 recommended IDs | First 3 items in list match recommended IDs |
| VM-FS-03 | Toggling favourite persists | `onFavouriteToggled(recipe)` | `saveRecipeUseCase` called once with correct recipe |
| VM-FS-04 | Filter by film simulation type | `onFilterChanged(FilmType.PROVIA)` | Only PROVIA recipes shown |
| VM-FS-05 | Saved recipes tab shows only favourites | `onTabSelected(Tab.SAVED)` | `uiState.recipes` all have `isFavourite == true` |

---

### 2.3 UseCase Tests

**File prefix**: `app/src/test/java/com/example/photofriend/domain/`

#### 2.3.1 GetCameraModelsUseCase

```kotlin
class GetCameraModelsUseCaseTest {
    private val repository: CameraRepository = mockk()
    private val useCase = GetCameraModelsUseCase(repository)

    @Test fun `returns flow from repository`() = runTest {
        every { repository.getCameraModels() } returns flowOf(TestData.allCameraModels)
        useCase().test {
            assertThat(awaitItem()).isEqualTo(TestData.allCameraModels)
            awaitComplete()
        }
    }

    @Test fun `propagates repository exception`() = runTest {
        every { repository.getCameraModels() } returns flow { throw RuntimeException("DB error") }
        useCase().test {
            awaitError()
        }
    }
}
```

#### 2.3.2 GetCameraSettingsUseCase

| Test ID | Test Name | Expected Outcome |
|---|---|---|
| UC-S-01 | Returns settings for known camera ID | Non-empty settings list |
| UC-S-02 | Returns empty list for unknown camera ID | Empty list, no exception |
| UC-S-03 | Settings are sorted by display order | `settings[n].order <= settings[n+1].order` |

#### 2.3.3 AnalyzeSceneUseCase

| Test ID | Test Name | Expected Outcome |
|---|---|---|
| UC-AI-01 | Encodes frame as base64 before sending | API client receives non-empty base64 string |
| UC-AI-02 | Builds prompt including selected camera name | Prompt string contains camera name |
| UC-AI-03 | Returns parsed AiSuggestion from response | Returns domain object, not raw JSON |
| UC-AI-04 | Throws `AiUnavailableException` on 5xx | Use case throws typed exception |
| UC-AI-05 | Throws `AiRateLimitException` on 429 | Use case throws typed exception |

#### 2.3.4 SaveRecipeUseCase / GetSavedRecipesUseCase

| Test ID | Test Name | Expected Outcome |
|---|---|---|
| UC-R-01 | Save recipe calls DAO insert | Repository `insert()` called once |
| UC-R-02 | Duplicate save is idempotent | No duplicate in saved list |
| UC-R-03 | Delete recipe calls DAO delete | Repository `delete()` called once |
| UC-R-04 | Get saved recipes returns flow | Flow emits updated list after each save/delete |

---

### 2.4 Repository Tests (Mocked Data Sources)

**File prefix**: `app/src/test/java/com/example/photofriend/data/repository/`

#### 2.4.1 CameraRepositoryImplTest

```kotlin
class CameraRepositoryImplTest {
    private val localDataSource: CameraLocalDataSource = mockk()
    private val remoteDataSource: CameraRemoteDataSource = mockk()
    private val repository = CameraRepositoryImpl(localDataSource, remoteDataSource)

    @Test fun `getCameraModels returns local when available`() = runTest {
        coEvery { localDataSource.getCameraModels() } returns TestData.allCameraModels
        repository.getCameraModels().test {
            assertThat(awaitItem()).isEqualTo(TestData.allCameraModels)
        }
    }

    @Test fun `getCameraModels fetches remote and caches on empty local`() = runTest {
        coEvery { localDataSource.getCameraModels() } returns emptyList()
        coEvery { remoteDataSource.getCameraModels() } returns TestData.allCameraModels
        coEvery { localDataSource.insertCameraModels(any()) } just Runs

        repository.getCameraModels().test {
            awaitItem()
            coVerify { localDataSource.insertCameraModels(TestData.allCameraModels) }
        }
    }
}
```

#### 2.4.2 AiRepositoryImplTest

| Test ID | Test Name | Expected Outcome |
|---|---|---|
| AR-01 | `analyzeScene()` constructs correct Anthropic API request | Request body has `model`, `messages`, `max_tokens` fields |
| AR-02 | Successful 200 response maps to `AiSuggestion` domain object | All fields populated |
| AR-03 | HTTP 401 maps to `AuthenticationException` | Typed exception thrown |
| AR-04 | HTTP 529 (overloaded) maps to `AiUnavailableException` | Typed exception thrown |
| AR-05 | Malformed JSON in response triggers parse error | `AiParseException` thrown, not crash |

---

### 2.5 AI Response Parsing / Validation Tests

This is a critical area because the Claude API returns free-form text that must be parsed into structured `CameraSettings` objects.

**File**: `app/src/test/java/com/example/photofriend/data/ai/AiResponseParserTest.kt`

```kotlin
class AiResponseParserTest {

    private val parser = AiResponseParser()

    @Test fun `parses well-formed JSON suggestion block`() {
        val raw = """
            Based on the scene, here are my recommendations:
            ```json
            {
              "aperture": "f/4.0",
              "shutter_speed": "1/250",
              "iso": "400",
              "white_balance": "Daylight",
              "film_simulation": "Velvia",
              "reasoning": "High contrast sunlit scene benefits from Velvia saturation."
            }
            ```
        """.trimIndent()

        val result = parser.parse(raw)

        assertThat(result.aperture).isEqualTo("f/4.0")
        assertThat(result.shutterSpeed).isEqualTo("1/250")
        assertThat(result.iso).isEqualTo("400")
        assertThat(result.filmSimulation).isEqualTo("Velvia")
        assertThat(result.reasoning).isNotEmpty()
    }

    @Test fun `returns fallback when no JSON block present`() {
        val result = parser.parse("I cannot analyze this image.")
        assertThat(result).isEqualTo(AiSuggestion.EMPTY)
    }

    @Test fun `handles missing optional fields gracefully`() {
        val raw = """```json { "aperture": "f/2.8", "film_simulation": "Classic Chrome" } ```"""
        val result = parser.parse(raw)
        assertThat(result.aperture).isEqualTo("f/2.8")
        assertThat(result.iso).isNull()  // optional field absent
    }

    @Test fun `rejects invalid aperture value`() {
        val raw = """```json { "aperture": "f/999", "film_simulation": "Velvia" } ```"""
        val result = parser.parse(raw)
        assertThat(result.validationErrors).contains(ValidationError.INVALID_APERTURE)
    }

    @Test fun `rejects shutter speed outside camera range`() {
        // X-T30 III max mechanical shutter: 1/4000
        val raw = """```json { "shutter_speed": "1/8000", "film_simulation": "Provia" } ```"""
        val result = parser.parseWithCamera(raw, TestData.xtFujifilm)
        assertThat(result.validationErrors).contains(ValidationError.SHUTTER_SPEED_OUT_OF_RANGE)
    }

    @Test fun `normalises ISO string variants`() {
        // API might return "ISO 400", "400", or "iso400"
        listOf("ISO 400", "400", "iso400", "ISO400").forEach { isoStr ->
            val raw = """```json { "iso": "$isoStr" } ```"""
            assertThat(parser.parse(raw).iso).isEqualTo("400")
        }
    }
}
```

---

### 2.6 Film Simulation Recipe Logic Tests

**File**: `app/src/test/java/com/example/photofriend/domain/FilmSimulationScorerTest.kt`

| Test ID | Test Name | Input | Expected Outcome |
|---|---|---|---|
| FS-L-01 | Golden hour scene recommends warm simulations | Scene tag = GOLDEN_HOUR | Eterna Cinema or Velvia in top 3 |
| FS-L-02 | Portrait scene avoids high-saturation simulations | Scene tag = PORTRAIT | Astia/Soft or Classic Neg in top 3 |
| FS-L-03 | Street / B&W mode returns Acros variants | Scene tag = STREET, preference = MONO | Acros-R, Acros-G, or Acros-Ye returned |
| FS-L-04 | Score is bounded 0–100 | Any input | `score in 0..100` |
| FS-L-05 | Recipes with missing grain setting still score | Recipe lacks grain field | No NullPointerException, score computed |
| FS-L-06 | Custom recipe overrides beat built-in when user has saved it | User has saved recipe matching scene | Saved recipe ranks first |

---

## 3. Integration Test Plan

Integration tests run on the JVM using real Room (in-memory database) and MockWebServer. Run with `./gradlew test`.

### 3.1 Dependencies to Add (libs.versions.toml)

```toml
[versions]
room        = "2.6.1"
mockwebserver = "4.12.0"

[libraries]
room-runtime    = { group = "androidx.room", name = "room-runtime",    version.ref = "room" }
room-ktx        = { group = "androidx.room", name = "room-ktx",        version.ref = "room" }
room-compiler   = { group = "androidx.room", name = "room-compiler",   version.ref = "room" }
room-testing    = { group = "androidx.room", name = "room-testing",    version.ref = "room" }
mockwebserver   = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "mockwebserver" }
```

---

### 3.2 Room Database Integration Tests

**File**: `app/src/test/java/com/example/photofriend/data/local/CameraModelDaoTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class CameraModelDaoTest {

    private lateinit var db: PhotofriendDatabase
    private lateinit var dao: CameraModelDao

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PhotofriendDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.cameraModelDao()
    }

    @After fun tearDown() = db.close()

    @Test fun insertAndRetrieve_returnsAllModels() = runTest {
        dao.insertAll(TestData.allCameraModels.map { it.toEntity() })
        val result = dao.getAll().first()
        assertThat(result).hasSize(TestData.allCameraModels.size)
    }

    @Test fun insertDuplicate_replacesExisting() = runTest {
        val model = TestData.xtFujifilm.toEntity()
        dao.insertAll(listOf(model))
        dao.insertAll(listOf(model.copy(displayName = "Updated Name")))
        val result = dao.getAll().first()
        assertThat(result).hasSize(1)
        assertThat(result.first().displayName).isEqualTo("Updated Name")
    }

    @Test fun getById_returnsCorrectModel() = runTest {
        dao.insertAll(TestData.allCameraModels.map { it.toEntity() })
        val result = dao.getById("xt30iii")
        assertThat(result?.id).isEqualTo("xt30iii")
    }

    @Test fun getById_unknownId_returnsNull() = runTest {
        val result = dao.getById("nonexistent")
        assertThat(result).isNull()
    }

    @Test fun delete_removesModel() = runTest {
        dao.insertAll(listOf(TestData.xtFujifilm.toEntity()))
        dao.deleteById("xt30iii")
        assertThat(dao.getAll().first()).isEmpty()
    }
}
```

#### FilmSimulationRecipeDao Tests

| Test ID | Test Name | Expected Outcome |
|---|---|---|
| DAO-R-01 | Insert recipe and retrieve | Recipe returned by `getAll()` |
| DAO-R-02 | Favourite toggle updates isFavourite flag | `getById()` returns entity with updated flag |
| DAO-R-03 | Get favourites filters non-favourite recipes | `getFavourites()` returns only `isFavourite == true` |
| DAO-R-04 | Search by film simulation type | `getByFilmType("Velvia")` returns only Velvia recipes |
| DAO-R-05 | Delete saved recipe removes it | Deleted recipe absent from `getAll()` |
| DAO-R-06 | Database survives process death simulation | Insert -> close db -> reopen -> data persists |

---

### 3.3 Repository Integration with Room

**File**: `app/src/test/java/com/example/photofriend/data/repository/FilmSimulationRepositoryIntegrationTest.kt`

| Test ID | Test Name | Expected Outcome |
|---|---|---|
| RI-01 | Repository flow emits on each insert | `getRecipes()` emits updated list |
| RI-02 | Repository flow emits on delete | `getRecipes()` emits list without deleted item |
| RI-03 | Concurrent inserts handled safely | All items present after parallel inserts |
| RI-04 | Cache-then-refresh pattern works | Stale local data replaced by fresh remote data |

---

### 3.4 API Client Integration Tests (MockWebServer)

**File**: `app/src/test/java/com/example/photofriend/data/remote/AnthropicApiClientTest.kt`

```kotlin
class AnthropicApiClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiClient: AnthropicApiClient

    @Before fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        apiClient = AnthropicApiClient.create(
            baseUrl = mockWebServer.url("/").toString(),
            apiKey = "test-key"
        )
    }

    @After fun tearDown() = mockWebServer.shutdown()

    @Test fun `successful response is parsed correctly`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Fixtures.VALID_CLAUDE_RESPONSE)
                .addHeader("Content-Type", "application/json")
        )
        val result = apiClient.analyzeScene(Fixtures.VALID_ANALYZE_REQUEST)
        assertThat(result.content).isNotEmpty()
    }

    @Test fun `request includes correct Anthropic headers`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(Fixtures.VALID_CLAUDE_RESPONSE))
        apiClient.analyzeScene(Fixtures.VALID_ANALYZE_REQUEST)
        val request = mockWebServer.takeRequest()
        assertThat(request.getHeader("x-api-key")).isEqualTo("test-key")
        assertThat(request.getHeader("anthropic-version")).isNotNull()
    }

    @Test fun `request body contains base64 image`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(Fixtures.VALID_CLAUDE_RESPONSE))
        apiClient.analyzeScene(Fixtures.VALID_ANALYZE_REQUEST)
        val body = mockWebServer.takeRequest().body.readUtf8()
        assertThat(body).contains("\"type\":\"image\"")
        assertThat(body).contains("\"media_type\":\"image/jpeg\"")
    }

    @Test fun `HTTP 401 throws AuthenticationException`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"type":"authentication_error"}}"""))
        assertThrows<AuthenticationException> {
            apiClient.analyzeScene(Fixtures.VALID_ANALYZE_REQUEST)
        }
    }

    @Test fun `HTTP 429 throws RateLimitException`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(429))
        assertThrows<AiRateLimitException> {
            apiClient.analyzeScene(Fixtures.VALID_ANALYZE_REQUEST)
        }
    }

    @Test fun `read timeout propagates as TimeoutException`() = runTest {
        mockWebServer.enqueue(MockResponse().setBodyDelay(35, TimeUnit.SECONDS))
        assertThrows<TimeoutException> {
            apiClient.analyzeScene(Fixtures.VALID_ANALYZE_REQUEST)
        }
    }
}
```

---

## 4. Instrumented / UI Test Plan

All tests in this section require a connected device or running emulator. Run with `./gradlew connectedAndroidTest`.

### 4.1 Dependencies to Add (libs.versions.toml)

```toml
[libraries]
compose-ui-test-junit4  = { group = "androidx.compose.ui", name = "ui-test-junit4" }
test-rules              = { group = "androidx.test",        name = "rules",       version = "1.5.0" }
hilt-testing            = { group = "com.google.dagger",   name = "hilt-android-testing", version.ref = "hilt" }
```

### 4.2 Test Setup

All instrumented tests use a `HiltAndroidRule` to inject a test `AppModule` that replaces the real Claude API client with a deterministic fake, and the real Room database with an in-memory variant.

```kotlin
@HiltAndroidTest
@UninstallModules(NetworkModule::class, DatabaseModule::class)
class BaseInstrumentedTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeTestRule = createAndroidComposeRule<MainActivity>()
}
```

---

### 4.3 Camera Permission Flow

**File**: `app/src/androidTest/java/com/example/photofriend/ui/permission/CameraPermissionFlowTest.kt`

| Test ID | Test Name | Steps | Expected Result |
|---|---|---|---|
| UI-P-01 | Permission rationale shown on first launch | Fresh install, open app | Permission rationale dialog/screen displayed |
| UI-P-02 | Granting permission shows camera viewfinder | Grant camera permission | CameraX preview surface visible |
| UI-P-03 | Denying permission shows error UI | Deny permission | Error message and "Open Settings" button visible |
| UI-P-04 | "Open Settings" deep-links to app settings | Tap "Open Settings" | Android app settings opens |
| UI-P-05 | Permission granted from settings returns to viewfinder | Grant from settings, return | Camera preview resumes |

```kotlin
@Test fun `permission denied shows error state with settings link`() {
    // Use GrantPermissionRule to deny — or test the denied branch via a fake permission provider
    composeTestRule.onNodeWithText("Camera access is required").assertIsDisplayed()
    composeTestRule.onNodeWithText("Open Settings").assertIsDisplayed().performClick()
    // Verify intent to settings was fired via ActivityScenario or IntentMatcher
}
```

---

### 4.4 Camera Model Selection Screen

**File**: `app/src/androidTest/java/com/example/photofriend/ui/cameraselection/CameraSelectionScreenTest.kt`

| Test ID | Test Name | Steps | Expected Result |
|---|---|---|---|
| UI-CS-01 | Camera list renders all models | Open selection screen | All seeded camera models visible in list |
| UI-CS-02 | Search field filters list | Type "X-T30" in search bar | Only X-T30 variants shown |
| UI-CS-03 | Clearing search restores full list | Clear search bar | Full list returns |
| UI-CS-04 | Tapping a camera navigates to settings | Tap "Fujifilm X-T30 III" | Settings screen opens for that model |
| UI-CS-05 | Camera name and sensor info displayed | List visible | Name, megapixels, sensor size shown per item |
| UI-CS-06 | Scroll performance on large list | List with 50+ models | No dropped frames (jank) during scroll |

```kotlin
@Test fun `search filters camera list`() {
    composeTestRule.onNodeWithTag("SearchField").performTextInput("X-T30")
    composeTestRule.onAllNodesWithTag("CameraListItem").assertAll(
        hasText("X-T30", substring = true)
    )
}
```

---

### 4.5 Settings Display and Filtering

**File**: `app/src/androidTest/java/com/example/photofriend/ui/settings/CameraSettingsScreenTest.kt`

| Test ID | Test Name | Steps | Expected Result |
|---|---|---|---|
| UI-SET-01 | All setting categories shown | Open settings for X-T30 III | Category chips: Exposure, Focus, Film, Color, Drive |
| UI-SET-02 | Tapping Exposure chip filters to exposure settings | Tap "Exposure" | Only aperture, shutter, ISO items shown |
| UI-SET-03 | Setting item shows name and current default value | List visible | Each row has label and value |
| UI-SET-04 | AI suggestion button is present | Settings screen loaded | "Analyze Scene" FAB or button visible |
| UI-SET-05 | Settings scroll without clipping | 20+ settings for camera | All settings reachable by scroll |
| UI-SET-06 | Tapping a setting opens value picker | Tap "ISO" setting | Picker/slider/dialog for ISO appears |

---

### 4.6 AI Suggestion Trigger and Display

**File**: `app/src/androidTest/java/com/example/photofriend/ui/ai/AiSuggestionScreenTest.kt`

Use the fake API client injected via Hilt test module that returns canned responses.

| Test ID | Test Name | Steps | Expected Result |
|---|---|---|---|
| UI-AI-01 | Tapping Analyze shows loading indicator | Tap "Analyze Scene" | Circular progress shown, button disabled |
| UI-AI-02 | Successful AI response populates suggestions | Fake API returns valid JSON | Aperture, shutter, ISO, film sim displayed |
| UI-AI-03 | Reasoning text is displayed | AI response includes reasoning | Reasoning paragraph visible |
| UI-AI-04 | Error state shows retry button | Fake API returns error | "Something went wrong" + "Retry" button shown |
| UI-AI-05 | Retry re-triggers analysis | Tap "Retry" | Loading indicator shows again |
| UI-AI-06 | Offline state shows no-internet message | No network mock | "No internet connection" message visible |
| UI-AI-07 | Apply suggestion updates settings display | Tap "Apply" on AI suggestion | Settings screen reflects suggested values |

---

### 4.7 Film Simulation Recipe Browsing and Saving

**File**: `app/src/androidTest/java/com/example/photofriend/ui/filmrecipe/FilmSimulationScreenTest.kt`

| Test ID | Test Name | Steps | Expected Result |
|---|---|---|---|
| UI-FS-01 | Recipe list shows name, film type, and thumbnail | Open film simulation screen | All three elements visible per card |
| UI-FS-02 | AI-recommended recipes shown at top | AI analysis done, recipes loaded | Recommended badge visible on top results |
| UI-FS-03 | Tapping recipe shows full detail | Tap recipe card | Detail sheet opens with all settings |
| UI-FS-04 | Save recipe button works | Tap heart/save on recipe | Toast or icon change confirms save |
| UI-FS-05 | Saved tab shows only favourites | Tap "Saved" tab | Only previously saved recipes shown |
| UI-FS-06 | Un-saving removes from Saved tab | Tap filled heart on saved recipe | Recipe disappears from Saved list |
| UI-FS-07 | Filter by film simulation type works | Tap "Velvia" filter | Only Velvia-based recipes shown |
| UI-FS-08 | Recipe detail includes all parameters | Open any recipe | DR, highlight, shadow, color, sharpness, noise visible |

---

### 4.8 Navigation Between All Screens

**File**: `app/src/androidTest/java/com/example/photofriend/ui/navigation/NavigationTest.kt`

| Test ID | Test Name | Steps | Expected Result |
|---|---|---|---|
| UI-NAV-01 | App launches to camera viewfinder | Cold launch | Camera preview screen is root |
| UI-NAV-02 | Viewfinder -> Camera Selection | Tap camera selector icon | Camera selection screen pushed |
| UI-NAV-03 | Camera Selection -> Settings | Tap any camera | Settings screen pushed |
| UI-NAV-04 | Settings -> Film Simulation | Tap Film Simulation tab/button | Film simulation screen shown |
| UI-NAV-05 | Back navigation unwinds stack | Press back from Settings | Returns to Camera Selection |
| UI-NAV-06 | Back from Camera Selection | Press back | Returns to Viewfinder |
| UI-NAV-07 | Deep link into recipe detail | Open app with recipe deep link URI | Recipe detail shown directly |
| UI-NAV-08 | Bottom nav retains state on tab switch | Switch tabs, switch back | Previous scroll position and state preserved |

---

## 5. Manual Test Cases

The following test cases require a human tester with a physical device. Priority: **P0** = blocker, **P1** = high, **P2** = medium, **P3** = low.

### 5.1 Camera and Viewfinder

| ID | Screen | Steps | Expected Result | Priority |
|---|---|---|---|---|
| MT-C-01 | Viewfinder | Launch app on cold start | Live camera preview visible within 2 seconds | P0 |
| MT-C-02 | Viewfinder | Rotate device to landscape | Preview rotates, no crash, no stretch | P0 |
| MT-C-03 | Viewfinder | Rotate back to portrait | Preview adjusts correctly | P0 |
| MT-C-04 | Viewfinder | Place phone face-down (cover camera) | App handles gracefully, no ANR | P1 |
| MT-C-05 | Viewfinder | Switch to front camera (if supported) | Front camera preview shown | P2 |
| MT-C-06 | Viewfinder | Move camera quickly (motion blur) | Preview remains responsive, no freeze | P1 |
| MT-C-07 | Viewfinder | Incoming call interrupts camera | Preview pauses, resumes after call | P1 |
| MT-C-08 | Viewfinder | Lock screen during camera use | Preview pauses; resumes on unlock | P1 |
| MT-C-09 | Viewfinder | Point at very bright light source | No crash, HDR or overexposure handled | P2 |

### 5.2 Camera Model Selection

| ID | Screen | Steps | Expected Result | Priority |
|---|---|---|---|---|
| MT-CS-01 | Camera Selection | Scroll through full model list | Smooth scroll, all models render | P1 |
| MT-CS-02 | Camera Selection | Search for "Fujifilm" | All Fujifilm models shown, others hidden | P1 |
| MT-CS-03 | Camera Selection | Search with all-caps "FUJIFILM" | Same results as lowercase search | P1 |
| MT-CS-04 | Camera Selection | Search with no matching results | Empty state message shown | P1 |
| MT-CS-05 | Camera Selection | Select X-T30 III | Settings screen opens for X-T30 III | P0 |
| MT-CS-06 | Camera Selection | Select different camera than previously selected | Settings update to reflect new camera | P1 |

### 5.3 Settings Display

| ID | Screen | Steps | Expected Result | Priority |
|---|---|---|---|---|
| MT-S-01 | Settings | Open settings for X-T30 III | All documented settings visible | P0 |
| MT-S-02 | Settings | Tap "Exposure" category | Only ISO, aperture, shutter speed settings shown | P1 |
| MT-S-03 | Settings | Tap "Film Simulation" category | Film sim settings shown | P1 |
| MT-S-04 | Settings | Change ISO value | Value updates in UI immediately | P1 |
| MT-S-05 | Settings | Rotate device while viewing settings | List state preserved after rotation | P1 |

### 5.4 AI Scene Analysis

| ID | Screen | Steps | Expected Result | Priority |
|---|---|---|---|---|
| MT-AI-01 | Viewfinder / AI | Point at sunlit outdoor scene, tap Analyze | AI returns aperture, shutter, ISO, film sim suggestions | P0 |
| MT-AI-02 | Viewfinder / AI | Point at dark indoor scene, tap Analyze | AI suggests higher ISO and wider aperture | P1 |
| MT-AI-03 | Viewfinder / AI | Point at portrait subject, tap Analyze | AI suggests appropriate portrait-friendly film sim | P1 |
| MT-AI-04 | Viewfinder / AI | Tap Analyze with no network | Offline error message shown within 3 seconds | P0 |
| MT-AI-05 | Viewfinder / AI | Tap Analyze twice rapidly | Second tap ignored or queued; no double request | P1 |
| MT-AI-06 | AI Result | Read suggestion reasoning text | Reasoning is coherent English, camera-specific | P2 |
| MT-AI-07 | AI Result | Tap "Apply Settings" | Settings screen reflects all suggested values | P1 |

### 5.5 Film Simulation Recipes

| ID | Screen | Steps | Expected Result | Priority |
|---|---|---|---|---|
| MT-FS-01 | Film Simulation | Open screen after AI analysis | AI-recommended recipes highlighted at top | P1 |
| MT-FS-02 | Film Simulation | Scroll through all recipes | All recipe cards render correctly | P1 |
| MT-FS-03 | Film Simulation | Tap recipe card | Detail sheet shows all settings | P0 |
| MT-FS-04 | Film Simulation | Save a recipe | Recipe appears in Saved tab | P0 |
| MT-FS-05 | Film Simulation | Save recipe, kill app, reopen | Saved recipe still present | P0 |
| MT-FS-06 | Film Simulation | Un-save a recipe | Recipe removed from Saved tab | P1 |
| MT-FS-07 | Film Simulation | Filter by Velvia | Only Velvia recipes shown | P2 |
| MT-FS-08 | Film Simulation | Rotate device while in recipe detail | Detail preserves state, no crash | P1 |

### 5.6 Accessibility

| ID | Screen | Steps | Expected Result | Priority |
|---|---|---|---|---|
| MT-A-01 | All screens | Enable TalkBack, navigate app | All interactive elements have content descriptions | P1 |
| MT-A-02 | All screens | Increase font size to largest | Text does not clip or overlap | P2 |
| MT-A-03 | All screens | Use high-contrast mode | UI remains usable | P2 |
| MT-A-04 | Camera Selection | Navigate list via TalkBack swipes | Each camera item is individually focusable | P1 |

---

## 6. Edge Cases and Risk Areas

### 6.1 Camera Permission Denied

**Risk level**: High — app is non-functional without camera access.

| Scenario | Expected Behavior | Test Coverage |
|---|---|---|
| First launch: user denies once | Show rationale dialog, allow retry | UI-P-01, MT-C-01 |
| First launch: user denies permanently ("Don't ask again") | Show persistent error UI with "Open Settings" CTA | UI-P-03, MT-C-01 |
| Permission revoked from OS settings while app is backgrounded | On foreground resume, detect revocation, show error UI | Manual only |
| Permission granted then revoked mid-session | Camera preview stops gracefully; no crash | Manual only |

**Implementation note**: `ActivityResultContracts.RequestPermission` result callback must handle all three states: granted, denied-retryable, denied-permanent.

---

### 6.2 No Internet Connection (AI Unavailable)

**Risk level**: High — AI is a core differentiator.

| Scenario | Expected Behavior | Test Coverage |
|---|---|---|
| No network at analysis time | Immediate "No internet connection" error with retry | UI-AI-06, MT-AI-04 |
| Network drops during API call | Timeout exception caught, error state shown | AR-01, MT-AI-04 |
| Network restored after offline error | Retry button triggers new successful request | UI-AI-05 |
| API key missing / invalid | Authentication error shown, not a crash | AR-03 |

**App must remain fully usable offline** for all non-AI features: camera viewfinder, settings display, saved recipes.

---

### 6.3 Unsupported Camera Model

**Risk level**: Medium — database may not cover all cameras a user searches for.

| Scenario | Expected Behavior | Test Coverage |
|---|---|---|
| User searches for model not in database | "Camera not found" empty state shown | MT-CS-04 |
| AI suggests settings incompatible with selected camera | Validation error shown alongside suggestion | AiResponseParserTest (AI-P-05) |
| Database returns camera with no settings | Settings screen shows "No settings available" empty state | VM-S-06 |

---

### 6.4 AI API Timeout / Error

**Risk level**: High — Claude API latency is variable.

| Scenario | Expected Behavior | Test Coverage |
|---|---|---|
| Response takes > 30 seconds | Request cancelled, timeout error shown | Integration: AR-06 |
| API returns HTTP 500 | Retry-eligible error message shown | AR-04, UI-AI-04 |
| API returns HTTP 529 (overloaded) | "Service busy, please try again" message shown | AR-04 |
| API returns HTTP 429 (rate limited) | "Rate limit reached" with cooldown indicator | AR-05 |
| API returns valid HTTP 200 but empty content | Parse error handled, fallback shown | AiResponseParserTest |
| Response contains unexpected JSON schema | Parser returns partial result, not crash | AiResponseParserTest |

**Timeout configuration**: Set OkHttp read timeout to 30 seconds. Apply exponential backoff with jitter for retries (max 2 automatic retries for 5xx).

---

### 6.5 Low-Light Scene Analysis

**Risk level**: Medium — image quality affects AI analysis quality.

| Scenario | Expected Behavior | Test Coverage |
|---|---|---|
| Very dark scene | CameraX preview still renders; AI analyzes (may return high-ISO suggestion) | Manual: MT-AI-02 |
| Completely black frame (lens covered) | AI returns graceful "unable to analyze" or high-ISO suggestion | Manual |
| Night scene with artificial light | AI correctly identifies warm vs. cool light, suggests appropriate WB | Manual |

**Note**: CameraX auto-exposure should handle dark scenes at the hardware level. The app should not artificially brighten the captured frame before sending to AI.

---

### 6.6 Device Rotation During Camera Preview

**Risk level**: High — rotation triggers Activity recreation by default.

| Scenario | Expected Behavior | Test Coverage |
|---|---|---|
| Rotate during idle viewfinder | Preview resumes within 1 second, no black flash | MT-C-02 |
| Rotate during AI analysis (pending) | Loading state preserved or gracefully restarted | Manual |
| Rotate while recipe detail sheet is open | Sheet remains open, content preserved | MT-FS-08 |
| Rapid rotation (multiple times) | No ANR, no memory leak, camera releases cleanly | Manual |

**Mitigation**: Use `ViewModel` to survive configuration changes. Use `rememberSaveable` for UI state that must persist. Use `ProcessCameraProvider` bound to `lifecycleOwner` to auto-manage CameraX lifecycle.

---

### 6.7 Additional Risk Areas

| Risk Area | Scenario | Mitigation |
|---|---|---|
| Memory — large images | Capturing 26MP frame for AI | Downsample to max 1024x1024 before encoding to base64 |
| Performance — main thread | Room query on main thread | All DB operations on `Dispatchers.IO` |
| Security — API key exposure | API key in APK | Store key server-side; use a thin proxy; never ship key in client code |
| Data migration — Room schema change | Schema version bump without migration | Define `Migration` objects; test with `MigrationTestHelper` |
| ANR — CameraX lifecycle | Not releasing camera on background | Bind CameraX to `ProcessLifecycleOwner` or `Activity` lifecycle |
| Battery drain | CameraX + background network polling | No background polling; all requests user-initiated |

---

## 7. Test Data

### 7.1 Camera Models

```kotlin
object TestData {

    val xtFujifilm = CameraModel(
        id = "xt30iii",
        brand = "Fujifilm",
        name = "X-T30 III",
        sensorType = "X-Trans CMOS 5 HR",
        megapixels = 40.2f,
        maxShutterSpeed = "1/4000",
        minShutterSpeed = "30",
        apertureRange = "f/1.0–f/22",
        isoRange = "125–51200",
        supportsFilmSimulation = true,
        filmSimulations = listOf(
            "Provia/Standard", "Velvia/Vivid", "Astia/Soft",
            "Classic Chrome", "Classic Neg", "Nostalgic Neg",
            "Eterna/Cinema", "Eterna Bleach Bypass", "Acros",
            "Acros+R", "Acros+G", "Acros+Ye", "Monochrome",
            "Sepia"
        )
    )

    val xt5Fujifilm = CameraModel(
        id = "xt5",
        brand = "Fujifilm",
        name = "X-T5",
        sensorType = "X-Trans CMOS 5 HR",
        megapixels = 40.2f,
        maxShutterSpeed = "1/8000",
        minShutterSpeed = "30",
        apertureRange = "f/1.0–f/22",
        isoRange = "125–51200",
        supportsFilmSimulation = true,
        filmSimulations = listOf(/* same as X-T30 III */)
    )

    val gfx100s = CameraModel(
        id = "gfx100s",
        brand = "Fujifilm",
        name = "GFX 100S",
        sensorType = "102MP GFX CMOS",
        megapixels = 102.0f,
        maxShutterSpeed = "1/4000",
        minShutterSpeed = "60",
        apertureRange = "f/2.0–f/22",
        isoRange = "100–12800",
        supportsFilmSimulation = true,
        filmSimulations = listOf(/* medium format subset */)
    )

    val allCameraModels = listOf(xtFujifilm, xt5Fujifilm, gfx100s)
}
```

### 7.2 Film Simulation Recipes

```kotlin
object RecipeTestData {

    val velviaSunrise = FilmSimulationRecipe(
        id = "velvia-sunrise-001",
        name = "Golden Sunrise",
        filmSimulation = "Velvia/Vivid",
        dynamicRange = "DR200",
        highlight = +2,
        shadow = -1,
        color = +4,
        sharpness = 0,
        noiseReduction = -2,
        grainEffect = "Weak",
        grainSize = "Small",
        whiteBalance = "Daylight",
        whiteBalanceShiftR = +2,
        whiteBalanceShiftB = -3,
        iso = "Auto up to 1600",
        exposureCompensation = "+1/3",
        sceneTag = "GOLDEN_HOUR",
        authorNote = "Vivid saturation for sunrise/sunset landscapes."
    )

    val classicChromStreet = FilmSimulationRecipe(
        id = "classicchrome-street-002",
        name = "Street Faded",
        filmSimulation = "Classic Chrome",
        dynamicRange = "DR400",
        highlight = -2,
        shadow = +1,
        color = -2,
        sharpness = -1,
        noiseReduction = -4,
        grainEffect = "Strong",
        grainSize = "Large",
        whiteBalance = "Auto",
        whiteBalanceShiftR = 0,
        whiteBalanceShiftB = 0,
        iso = "Auto up to 6400",
        exposureCompensation = "0",
        sceneTag = "STREET",
        authorNote = "Faded, desaturated street look reminiscent of slide film."
    )

    val acrosPortrait = FilmSimulationRecipe(
        id = "acros-portrait-003",
        name = "Acros Soft Portrait",
        filmSimulation = "Acros",
        dynamicRange = "DR200",
        highlight = -1,
        shadow = -1,
        color = 0,          // irrelevant for mono but stored
        sharpness = -1,
        noiseReduction = -2,
        grainEffect = "Weak",
        grainSize = "Small",
        whiteBalance = "Auto",
        whiteBalanceShiftR = 0,
        whiteBalanceShiftB = 0,
        iso = "Auto up to 3200",
        exposureCompensation = "+1/3",
        sceneTag = "PORTRAIT",
        authorNote = "Smooth tonal gradation for B&W portraits."
    )

    val allRecipes = listOf(velviaSunrise, classicChromStreet, acrosPortrait)
}
```

### 7.3 AI Response Fixtures

```kotlin
object Fixtures {

    val VALID_CLAUDE_RESPONSE = """
        {
          "id": "msg_01abc",
          "type": "message",
          "role": "assistant",
          "content": [
            {
              "type": "text",
              "text": "Based on the scene, here are my recommendations:\n```json\n{\n  \"aperture\": \"f/4.0\",\n  \"shutter_speed\": \"1/500\",\n  \"iso\": \"200\",\n  \"white_balance\": \"Daylight\",\n  \"film_simulation\": \"Velvia/Vivid\",\n  \"dynamic_range\": \"DR200\",\n  \"reasoning\": \"The scene shows a bright outdoor landscape with strong directional sunlight. Velvia will enhance the saturation of the sky and foliage. f/4.0 provides sufficient depth of field for a landscape composition.\"\n}\n```"
            }
          ],
          "model": "claude-opus-4-6",
          "stop_reason": "end_turn"
        }
    """.trimIndent()

    val ERROR_401_RESPONSE = """{"error":{"type":"authentication_error","message":"Invalid API key"}}"""
    val ERROR_429_RESPONSE = """{"error":{"type":"rate_limit_error","message":"Rate limit exceeded"}}"""
    val ERROR_529_RESPONSE = """{"error":{"type":"overloaded_error","message":"Service temporarily overloaded"}}"""

    val MALFORMED_RESPONSE = """
        {
          "content": [{ "type": "text", "text": "I cannot determine camera settings from this image." }]
        }
    """.trimIndent()
}
```

---

## 8. Definition of Done

A feature is considered shippable when **all** of the following criteria are met:

### 8.1 Code Quality

- [ ] All new code reviewed and approved by at least one other developer
- [ ] No Kotlin compiler warnings introduced
- [ ] `./gradlew lint` passes with zero new issues at severity `Error` or `Warning`
- [ ] New code follows MVVM + Clean Architecture conventions (no business logic in Composables or Activities)

### 8.2 Unit Tests

- [ ] All new ViewModel, UseCase, Repository, and Parser classes have corresponding unit tests
- [ ] Unit test coverage for the new feature's logic layer is >= 80%
- [ ] `./gradlew test` passes with zero failures
- [ ] All edge case inputs tested (null, empty, out-of-range, malformed)

### 8.3 Integration Tests

- [ ] Room DAO CRUD operations for any new entities are integration-tested
- [ ] API client integration tests cover success, auth error, rate limit, timeout, and malformed response
- [ ] `./gradlew test` (integration scope) passes with zero failures

### 8.4 UI / Instrumented Tests

- [ ] Happy path automated UI test exists for every new user-facing screen
- [ ] Navigation to and from the new screen is covered
- [ ] `./gradlew connectedAndroidTest` passes on API 24 emulator and API 35 emulator

### 8.5 Manual QA Sign-off

- [ ] All P0 and P1 manual test cases in this plan pass on a physical device
- [ ] Tested on at minimum: one low-end device (RAM <= 3 GB, Android 7.x) and one current flagship (Android 14+)
- [ ] No P0 or P1 defects open
- [ ] P2 and P3 defects triaged and either fixed or deferred with product approval

### 8.6 Non-Functional

- [ ] App launch to viewfinder < 3 seconds on mid-range device (cold start)
- [ ] Camera preview frame rate >= 30 fps under normal conditions
- [ ] AI analysis response displayed within 15 seconds on fast network (not blocking UI)
- [ ] No memory leaks detected (LeakCanary shows zero retained objects after navigating away)
- [ ] Accessibility: all interactive elements have content descriptions (verified via Accessibility Scanner)

---

## 9. CI/CD Recommendations

### 9.1 On Every Pull Request

Run these checks in the PR pipeline (must all pass before merge):

| Step | Command | Timeout | Notes |
|---|---|---|---|
| Compile check | `./gradlew assembleDebug` | 5 min | Catches compilation errors early |
| Lint | `./gradlew lint` | 3 min | Block merge on new Error-severity issues |
| Unit tests | `./gradlew test` | 10 min | Includes unit + integration tests |
| Test coverage report | `./gradlew jacocoTestReport` | 2 min | Fail if feature coverage < 80% |

**Total PR gate budget**: ~20 minutes. All run in parallel where possible.

### 9.2 On Merge to Main

Run on every merge to the `main` branch (post-merge, non-blocking for the merger but must fix if broken):

| Step | Command | Notes |
|---|---|---|
| Full unit + integration tests | `./gradlew test` | Same as PR gate |
| Instrumented tests (API 24 emulator) | `./gradlew connectedAndroidTest` | Firebase Test Lab or GitHub Actions with emulator |
| Instrumented tests (API 35 emulator) | `./gradlew connectedAndroidTest` | Cover target SDK |
| APK size check | `bundleDebug` + size assertion | Alert if APK grows > 5 MB from baseline |
| Static analysis (Detekt) | `./gradlew detekt` | Non-blocking initially; enforce after baseline established |

### 9.3 Nightly

Run on a schedule (e.g., 02:00 UTC) against the current `main`:

| Step | Notes |
|---|---|
| Full instrumented test suite on physical device matrix | Use Firebase Test Lab: cover API 24, 28, 33, 35; at least 3 distinct device models |
| Monkey test (random input stress test) | `adb shell monkey -p com.example.photofriend -v 5000` — verifies no crash under random input |
| Memory leak scan | Build with LeakCanary assertions; fail on retained objects |
| Performance baseline | Measure startup time, camera preview time-to-frame, scroll jank; alert on regression |
| AI integration smoke test (real API) | One real call to Claude API with a known fixture image; verify parseable response |

### 9.4 Recommended CI Service Configuration

```yaml
# Example GitHub Actions matrix (excerpt)
strategy:
  matrix:
    api-level: [24, 35]
    target: [default, google_apis]

steps:
  - name: Run unit tests
    run: ./gradlew test

  - name: Run instrumented tests
    uses: reactivecircus/android-emulator-runner@v2
    with:
      api-level: ${{ matrix.api-level }}
      target: ${{ matrix.target }}
      arch: x86_64
      script: ./gradlew connectedAndroidTest
```

### 9.5 Test Reporting

- Publish JUnit XML reports to the CI dashboard on every run.
- Publish Compose UI test screenshots on failure.
- Track test flakiness rate; quarantine any test with a >5% failure rate until root-caused.
- Maintain a test results trend chart to catch gradual regressions.

---

## Appendix A: Test File Structure

```
app/src/
├── test/java/com/example/photofriend/
│   ├── data/
│   │   ├── ai/
│   │   │   └── AiResponseParserTest.kt
│   │   ├── local/
│   │   │   ├── CameraModelDaoTest.kt
│   │   │   └── FilmSimulationRecipeDaoTest.kt
│   │   ├── remote/
│   │   │   └── AnthropicApiClientTest.kt
│   │   └── repository/
│   │       ├── CameraRepositoryImplTest.kt
│   │       ├── AiRepositoryImplTest.kt
│   │       └── FilmSimulationRepositoryIntegrationTest.kt
│   ├── domain/
│   │   ├── GetCameraModelsUseCaseTest.kt
│   │   ├── GetCameraSettingsUseCaseTest.kt
│   │   ├── AnalyzeSceneUseCaseTest.kt
│   │   ├── SaveRecipeUseCaseTest.kt
│   │   └── FilmSimulationScorerTest.kt
│   ├── ui/
│   │   ├── cameraselection/
│   │   │   └── CameraSelectionViewModelTest.kt
│   │   ├── settings/
│   │   │   └── CameraSettingsViewModelTest.kt
│   │   ├── ai/
│   │   │   └── AiSuggestionViewModelTest.kt
│   │   └── filmrecipe/
│   │       └── FilmSimulationViewModelTest.kt
│   └── fixtures/
│       ├── TestData.kt
│       ├── RecipeTestData.kt
│       └── Fixtures.kt
│
└── androidTest/java/com/example/photofriend/
    ├── di/
    │   └── TestAppModule.kt
    ├── ui/
    │   ├── permission/
    │   │   └── CameraPermissionFlowTest.kt
    │   ├── cameraselection/
    │   │   └── CameraSelectionScreenTest.kt
    │   ├── settings/
    │   │   └── CameraSettingsScreenTest.kt
    │   ├── ai/
    │   │   └── AiSuggestionScreenTest.kt
    │   ├── filmrecipe/
    │   │   └── FilmSimulationScreenTest.kt
    │   └── navigation/
    │       └── NavigationTest.kt
    └── fixtures/
        └── InstrumentedTestData.kt
```

---

*End of QA Test Plan — Photofriend v1.0*
