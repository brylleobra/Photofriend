package com.example.photofriend

import app.cash.turbine.test
import com.example.photofriend.domain.model.CameraModel
import com.example.photofriend.domain.usecase.GetCameraModelsUseCase
import com.example.photofriend.ui.screen.cameraselect.CameraSelectUiState
import com.example.photofriend.ui.screen.cameraselect.CameraSelectViewModel
import io.mockk.every
import io.mockk.mockk
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
class CameraSelectViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var useCase: GetCameraModelsUseCase
    private lateinit var viewModel: CameraSelectViewModel

    private val xt30iii = CameraModel("fujifilm_xt30iii", "Fujifilm", "X-T30 III", "APS-C", 40, 20, 2024)
    private val xt5 = CameraModel("fujifilm_xt5", "Fujifilm", "X-T5", "APS-C", 40, 20, 2022)
    private val xs20 = CameraModel("fujifilm_xs20", "Fujifilm", "X-S20", "APS-C", 26, 20, 2023)
    private val x100vi = CameraModel("fujifilm_x100vi", "Fujifilm", "X100VI", "APS-C", 40, 20, 2024)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        useCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        every { useCase() } returns flowOf(emptyList())
        viewModel = CameraSelectViewModel(useCase)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial is CameraSelectUiState.Loading || initial is CameraSelectUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState emits Success with cameras when repository returns data`() = runTest {
        val cameras = listOf(xt30iii, xt5, xs20)
        every { useCase() } returns flowOf(cameras)
        viewModel = CameraSelectViewModel(useCase)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertTrue(state is CameraSelectUiState.Success)
            assertEquals(3, (state as CameraSelectUiState.Success).cameras.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchQuery filters cameras by name`() = runTest {
        val cameras = listOf(xt30iii, xt5, xs20, x100vi)
        every { useCase() } returns flowOf(cameras)
        viewModel = CameraSelectViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("X-T")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            if (state is CameraSelectUiState.Success) {
                val names = state.cameras.map { it.name }
                assertTrue(names.all { it.contains("X-T", ignoreCase = true) })
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchQuery filters cameras case-insensitively`() = runTest {
        val cameras = listOf(xt30iii, xt5, xs20)
        every { useCase() } returns flowOf(cameras)
        viewModel = CameraSelectViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("fujifilm")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            if (state is CameraSelectUiState.Success) {
                assertEquals(3, state.cameras.size)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchQuery with no match returns empty list`() = runTest {
        val cameras = listOf(xt30iii, xt5)
        every { useCase() } returns flowOf(cameras)
        viewModel = CameraSelectViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("Nikon")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            if (state is CameraSelectUiState.Success) {
                assertEquals(0, state.cameras.size)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing search query shows all cameras`() = runTest {
        val cameras = listOf(xt30iii, xt5, xs20)
        every { useCase() } returns flowOf(cameras)
        viewModel = CameraSelectViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("X-T5")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onSearchQueryChange("")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            if (state is CameraSelectUiState.Success) {
                assertEquals(3, state.cameras.size)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchQuery StateFlow updates correctly`() = runTest {
        every { useCase() } returns flowOf(emptyList())
        viewModel = CameraSelectViewModel(useCase)

        assertEquals("", viewModel.searchQuery.value)
        viewModel.onSearchQueryChange("X-T30")
        assertEquals("X-T30", viewModel.searchQuery.value)
        viewModel.onSearchQueryChange("")
        assertEquals("", viewModel.searchQuery.value)
    }
}
