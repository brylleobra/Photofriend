package com.example.photofriend

import app.cash.turbine.test
import com.example.photofriend.di.SettingsStore
import com.example.photofriend.domain.model.CameraSetting
import com.example.photofriend.domain.model.SettingCategory
import com.example.photofriend.domain.usecase.GetCameraSettingsUseCase
import com.example.photofriend.ui.screen.settings.CameraSettingsUiState
import com.example.photofriend.ui.screen.settings.CameraSettingsViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var useCase: GetCameraSettingsUseCase
    private lateinit var settingsStore: SettingsStore
    private lateinit var viewModel: CameraSettingsViewModel

    private val filmSimSetting = CameraSetting(
        id = "xt30iii_film_sim",
        cameraId = "fujifilm_xt30iii",
        category = SettingCategory.COLOR,
        name = "Film Simulation",
        description = "Emulates film stock characteristics",
        options = listOf("Provia/Standard", "Velvia/Vivid", "Classic Chrome"),
        defaultValue = "Provia/Standard"
    )

    private val grainSetting = CameraSetting(
        id = "xt30iii_grain",
        cameraId = "fujifilm_xt30iii",
        category = SettingCategory.COLOR,
        name = "Grain Effect",
        description = "Adds film grain",
        options = listOf("Off", "Weak Small", "Strong Small"),
        defaultValue = "Off"
    )

    private val isoSetting = CameraSetting(
        id = "xt30iii_iso",
        cameraId = "fujifilm_xt30iii",
        category = SettingCategory.EXPOSURE,
        name = "ISO",
        description = "Sensor sensitivity",
        options = listOf("Auto", "160", "400", "1600"),
        defaultValue = "Auto"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        useCase = mockk()
        settingsStore = mockk()
        coEvery { settingsStore.setValue(any(), any(), any()) } just Runs
        coEvery { settingsStore.resetCamera(any()) } just Runs
        viewModel = CameraSettingsViewModel(useCase, settingsStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState emits Success with settings grouped by category`() = runTest {
        val cameraId = "fujifilm_xt30iii"
        every { useCase(cameraId) } returns flowOf(listOf(filmSimSetting, grainSetting, isoSetting))
        every { settingsStore.getValuesFlow(cameraId) } returns flowOf(emptyMap())

        viewModel.init(cameraId)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertTrue(state is CameraSettingsUiState.Success)
            val success = state as CameraSettingsUiState.Success
            assertEquals(2, success.settingsByCategory[SettingCategory.COLOR]?.size)
            assertEquals(1, success.settingsByCategory[SettingCategory.EXPOSURE]?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `default values are used before any changes`() = runTest {
        val cameraId = "fujifilm_xt30iii"
        every { useCase(cameraId) } returns flowOf(listOf(filmSimSetting, grainSetting))
        every { settingsStore.getValuesFlow(cameraId) } returns flowOf(emptyMap())

        viewModel.init(cameraId)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            if (state is CameraSettingsUiState.Success) {
                assertEquals("Provia/Standard", state.selectedValues["xt30iii_film_sim"])
                assertEquals("Off", state.selectedValues["xt30iii_grain"])
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSettingChanged stages pending change visible in uiState`() = runTest {
        val cameraId = "fujifilm_xt30iii"
        every { useCase(cameraId) } returns flowOf(listOf(filmSimSetting, grainSetting))
        every { settingsStore.getValuesFlow(cameraId) } returns flowOf(emptyMap())

        viewModel.init(cameraId)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onSettingChanged("xt30iii_film_sim", "Velvia/Vivid")
            testDispatcher.scheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            if (state is CameraSettingsUiState.Success) {
                assertEquals("Velvia/Vivid", state.selectedValues["xt30iii_film_sim"])
                assertEquals("Off", state.selectedValues["xt30iii_grain"]) // unchanged
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSettingChanged can update multiple settings independently`() = runTest {
        val cameraId = "fujifilm_xt30iii"
        every { useCase(cameraId) } returns flowOf(listOf(filmSimSetting, grainSetting, isoSetting))
        every { settingsStore.getValuesFlow(cameraId) } returns flowOf(emptyMap())

        viewModel.init(cameraId)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onSettingChanged("xt30iii_film_sim", "Classic Chrome")
            viewModel.onSettingChanged("xt30iii_grain", "Weak Small")
            viewModel.onSettingChanged("xt30iii_iso", "400")
            testDispatcher.scheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            if (state is CameraSettingsUiState.Success) {
                assertEquals("Classic Chrome", state.selectedValues["xt30iii_film_sim"])
                assertEquals("Weak Small", state.selectedValues["xt30iii_grain"])
                assertEquals("400", state.selectedValues["xt30iii_iso"])
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resetAll clears pending changes reverting to defaults`() = runTest {
        val cameraId = "fujifilm_xt30iii"
        every { useCase(cameraId) } returns flowOf(listOf(filmSimSetting, grainSetting))
        every { settingsStore.getValuesFlow(cameraId) } returns flowOf(emptyMap())

        viewModel.init(cameraId)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onSettingChanged("xt30iii_film_sim", "Velvia/Vivid")
            viewModel.onSettingChanged("xt30iii_grain", "Strong Small")
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.resetAll()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            if (state is CameraSettingsUiState.Success) {
                assertEquals("Provia/Standard", state.selectedValues["xt30iii_film_sim"])
                assertEquals("Off", state.selectedValues["xt30iii_grain"])
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState emits Loading initially before init is called`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(
                "Expected Loading before init, got $initial",
                initial is CameraSettingsUiState.Loading
            )
            cancelAndIgnoreRemainingEvents()
        }
    }
}
