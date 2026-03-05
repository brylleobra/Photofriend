package com.example.photofriend

import com.example.photofriend.domain.model.FilmSimulationRecipe
import com.example.photofriend.domain.repository.CameraRepository
import com.example.photofriend.domain.usecase.GetFilmSimulationRecipesUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetFilmSimulationRecipesUseCaseTest {

    private lateinit var repository: CameraRepository
    private lateinit var useCase: GetFilmSimulationRecipesUseCase

    private fun makeRecipe(id: String, name: String, filmSim: String) = FilmSimulationRecipe(
        id = id,
        name = name,
        description = "Test recipe",
        filmSimulation = filmSim,
        grain = "Off",
        colorChrome = "Off",
        colorChromeBlue = "Off",
        whiteBalance = "Auto",
        wbShiftR = 0,
        wbShiftB = 0,
        highlights = 0,
        shadows = 0,
        color = 0,
        sharpness = 0,
        noiseReduction = 0,
        isoMin = 160,
        isoMax = 3200,
        tags = listOf("test"),
        isUserSaved = false
    )

    private val vintageKodachrome = makeRecipe("recipe_vintage_kodachrome", "Vintage Kodachrome", "Velvia/Vivid")
    private val moodyMono = makeRecipe("recipe_moody_monochrome", "Moody Monochrome", "Acros+R")
    private val savedRecipe = makeRecipe("recipe_user_1", "My Recipe", "Classic Chrome").copy(isUserSaved = true)

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetFilmSimulationRecipesUseCase(repository)
    }

    @Test
    fun `builtIn returns built-in recipes from repository`() = runTest {
        every { repository.getBuiltInRecipes() } returns flowOf(listOf(vintageKodachrome, moodyMono))

        val result = useCase.builtIn().toList().flatten()

        verify(exactly = 1) { repository.getBuiltInRecipes() }
        assertEquals(2, result.size)
    }

    @Test
    fun `saved returns saved recipes from repository`() = runTest {
        every { repository.getSavedRecipes() } returns flowOf(listOf(savedRecipe))

        val result = useCase.saved().toList().flatten()

        verify(exactly = 1) { repository.getSavedRecipes() }
        assertEquals(1, result.size)
        assertTrue(result[0].isUserSaved)
    }

    @Test
    fun `builtIn and saved are independent flows`() = runTest {
        every { repository.getBuiltInRecipes() } returns flowOf(listOf(vintageKodachrome))
        every { repository.getSavedRecipes() } returns flowOf(listOf(savedRecipe))

        val builtIn = useCase.builtIn().toList().flatten()
        val saved = useCase.saved().toList().flatten()

        assertEquals(1, builtIn.size)
        assertEquals(1, saved.size)
        assertEquals("recipe_vintage_kodachrome", builtIn[0].id)
        assertEquals("recipe_user_1", saved[0].id)
    }

    @Test
    fun `builtIn returns empty list when no built-in recipes exist`() = runTest {
        every { repository.getBuiltInRecipes() } returns flowOf(emptyList())

        val result = useCase.builtIn().toList().flatten()

        assertEquals(0, result.size)
    }
}
