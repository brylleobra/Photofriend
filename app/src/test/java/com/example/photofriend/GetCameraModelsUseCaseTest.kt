package com.example.photofriend

import com.example.photofriend.domain.model.CameraModel
import com.example.photofriend.domain.repository.CameraRepository
import com.example.photofriend.domain.usecase.GetCameraModelsUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetCameraModelsUseCaseTest {

    private lateinit var repository: CameraRepository
    private lateinit var useCase: GetCameraModelsUseCase

    private val fujiXT30III = CameraModel(
        id = "fujifilm_xt30iii",
        brand = "Fujifilm",
        name = "X-T30 III",
        sensorSize = "APS-C",
        megapixels = 40,
        filmSimulationCount = 20,
        releaseYear = 2024
    )

    private val fujiXT5 = CameraModel(
        id = "fujifilm_xt5",
        brand = "Fujifilm",
        name = "X-T5",
        sensorSize = "APS-C",
        megapixels = 40,
        filmSimulationCount = 20,
        releaseYear = 2022
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetCameraModelsUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository getCameraModels`() = runTest {
        every { repository.getCameraModels() } returns flowOf(listOf(fujiXT30III, fujiXT5))

        val result = useCase().toList()

        verify(exactly = 1) { repository.getCameraModels() }
        assertEquals(1, result.size)
        assertEquals(2, result[0].size)
    }

    @Test
    fun `invoke returns all cameras from repository`() = runTest {
        val expected = listOf(fujiXT30III, fujiXT5)
        every { repository.getCameraModels() } returns flowOf(expected)

        val result = useCase().toList().flatten()

        assertEquals(expected, result)
    }

    @Test
    fun `invoke returns empty list when repository has no cameras`() = runTest {
        every { repository.getCameraModels() } returns flowOf(emptyList())

        val result = useCase().toList().flatten()

        assertEquals(0, result.size)
    }

    @Test
    fun `invoke emits multiple updates from repository`() = runTest {
        every { repository.getCameraModels() } returns
            kotlinx.coroutines.flow.flow {
                emit(listOf(fujiXT30III))
                emit(listOf(fujiXT30III, fujiXT5))
            }

        val emissions = useCase().toList()

        assertEquals(2, emissions.size)
        assertEquals(1, emissions[0].size)
        assertEquals(2, emissions[1].size)
    }
}
