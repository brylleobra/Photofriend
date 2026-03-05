package com.example.photofriend

import com.example.photofriend.domain.model.CameraSetting
import com.example.photofriend.domain.model.SettingCategory
import com.example.photofriend.domain.repository.CameraRepository
import com.example.photofriend.domain.usecase.GetCameraSettingsUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetCameraSettingsUseCaseTest {

    private lateinit var repository: CameraRepository
    private lateinit var useCase: GetCameraSettingsUseCase

    private val filmSimSetting = CameraSetting(
        id = "xt30iii_film_sim",
        cameraId = "fujifilm_xt30iii",
        category = SettingCategory.COLOR,
        name = "Film Simulation",
        description = "Emulates the color and tonal characteristics of Fujifilm film stocks",
        options = listOf("Provia/Standard", "Velvia/Vivid", "Astia/Soft", "Classic Chrome"),
        defaultValue = "Provia/Standard"
    )

    private val grainSetting = CameraSetting(
        id = "xt30iii_grain",
        cameraId = "fujifilm_xt30iii",
        category = SettingCategory.COLOR,
        name = "Grain Effect",
        description = "Adds film grain texture to images",
        options = listOf("Off", "Weak Small", "Weak Large", "Strong Small", "Strong Large"),
        defaultValue = "Off"
    )

    private val isoSetting = CameraSetting(
        id = "xt30iii_iso",
        cameraId = "fujifilm_xt30iii",
        category = SettingCategory.EXPOSURE,
        name = "ISO",
        description = "Controls sensor sensitivity to light",
        options = listOf("Auto", "160", "200", "400", "800", "1600", "3200", "6400"),
        defaultValue = "Auto"
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetCameraSettingsUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository with correct cameraId`() = runTest {
        val cameraId = "fujifilm_xt30iii"
        every { repository.getCameraSettings(cameraId) } returns flowOf(listOf(filmSimSetting))

        useCase(cameraId).toList()

        verify(exactly = 1) { repository.getCameraSettings(cameraId) }
    }

    @Test
    fun `invoke returns settings for the specified camera`() = runTest {
        val cameraId = "fujifilm_xt30iii"
        val expected = listOf(filmSimSetting, grainSetting, isoSetting)
        every { repository.getCameraSettings(cameraId) } returns flowOf(expected)

        val result = useCase(cameraId).toList().flatten()

        assertEquals(3, result.size)
        assertEquals(expected, result)
    }

    @Test
    fun `invoke returns empty list for camera with no settings`() = runTest {
        val cameraId = "fujifilm_xt5"
        every { repository.getCameraSettings(cameraId) } returns flowOf(emptyList())

        val result = useCase(cameraId).toList().flatten()

        assertEquals(0, result.size)
    }

    @Test
    fun `settings have correct categories`() = runTest {
        val cameraId = "fujifilm_xt30iii"
        every { repository.getCameraSettings(cameraId) } returns
            flowOf(listOf(filmSimSetting, grainSetting, isoSetting))

        val result = useCase(cameraId).toList().flatten()

        val colorSettings = result.filter { it.category == SettingCategory.COLOR }
        val exposureSettings = result.filter { it.category == SettingCategory.EXPOSURE }
        assertEquals(2, colorSettings.size)
        assertEquals(1, exposureSettings.size)
    }

    @Test
    fun `settings options are non-empty`() = runTest {
        val cameraId = "fujifilm_xt30iii"
        every { repository.getCameraSettings(cameraId) } returns
            flowOf(listOf(filmSimSetting, grainSetting))

        val result = useCase(cameraId).toList().flatten()

        result.forEach { setting ->
            assert(setting.options.isNotEmpty()) { "Setting '${setting.name}' has no options" }
        }
    }
}
