package com.example.photofriend

import com.example.photofriend.data.local.db.entity.CameraModelEntity
import com.example.photofriend.data.local.db.entity.CameraSettingEntity
import com.example.photofriend.data.local.db.entity.RecipeEntity
import com.example.photofriend.data.mapper.toDomain
import com.example.photofriend.data.mapper.toEntity
import com.example.photofriend.domain.model.FilmSimulationRecipe
import com.example.photofriend.domain.model.SettingCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraMapperTest {

    @Test
    fun `CameraModelEntity toDomain maps all fields correctly`() {
        val entity = CameraModelEntity(
            id = "fujifilm_xt30iii",
            brand = "Fujifilm",
            name = "X-T30 III",
            sensorSize = "APS-C",
            megapixels = 40,
            filmSimulationCount = 20,
            releaseYear = 2024
        )

        val domain = entity.toDomain()

        assertEquals("fujifilm_xt30iii", domain.id)
        assertEquals("Fujifilm", domain.brand)
        assertEquals("X-T30 III", domain.name)
        assertEquals("APS-C", domain.sensorSize)
        assertEquals(40, domain.megapixels)
        assertEquals(20, domain.filmSimulationCount)
        assertEquals(2024, domain.releaseYear)
    }

    @Test
    fun `CameraSettingEntity toDomain splits pipe-delimited options into list`() {
        val entity = CameraSettingEntity(
            id = "xt30iii_film_sim",
            cameraId = "fujifilm_xt30iii",
            category = "COLOR",
            name = "Film Simulation",
            description = "Emulates film stock",
            options = "Provia/Standard|Velvia/Vivid|Classic Chrome",
            defaultValue = "Provia/Standard"
        )

        val domain = entity.toDomain()

        assertEquals(3, domain.options.size)
        assertEquals("Provia/Standard", domain.options[0])
        assertEquals("Velvia/Vivid", domain.options[1])
        assertEquals("Classic Chrome", domain.options[2])
    }

    @Test
    fun `CameraSettingEntity toDomain maps category enum correctly`() {
        val colorEntity = CameraSettingEntity("1", "cam1", "COLOR", "Film Sim", "desc", "A|B", "A")
        val exposureEntity = CameraSettingEntity("2", "cam1", "EXPOSURE", "ISO", "desc", "Auto|160", "Auto")
        val outputEntity = CameraSettingEntity("3", "cam1", "OUTPUT", "Sharpness", "desc", "0|+1", "0")
        val unknownEntity = CameraSettingEntity("4", "cam1", "UNKNOWN_CATEGORY", "X", "desc", "A", "A")

        assertEquals(SettingCategory.COLOR, colorEntity.toDomain().category)
        assertEquals(SettingCategory.EXPOSURE, exposureEntity.toDomain().category)
        assertEquals(SettingCategory.OUTPUT, outputEntity.toDomain().category)
        assertEquals(SettingCategory.OUTPUT, unknownEntity.toDomain().category) // default fallback
    }

    @Test
    fun `RecipeEntity toDomain splits comma-separated tags into list`() {
        val entity = RecipeEntity(
            id = "recipe_1",
            name = "Vintage",
            description = "desc",
            filmSimulation = "Velvia/Vivid",
            grain = "Weak Small",
            colorChrome = "Strong",
            colorChromeBlue = "Weak",
            whiteBalance = "Daylight",
            wbShiftR = 2,
            wbShiftB = -1,
            highlights = -1,
            shadows = 1,
            color = 2,
            sharpness = 0,
            noiseReduction = -1,
            isoMin = 160,
            isoMax = 1600,
            tags = "vintage,warm,saturated",
            isBuiltIn = true,
            isUserSaved = false
        )

        val domain = entity.toDomain()

        assertEquals(3, domain.tags.size)
        assertTrue(domain.tags.contains("vintage"))
        assertTrue(domain.tags.contains("warm"))
        assertTrue(domain.tags.contains("saturated"))
    }

    @Test
    fun `FilmSimulationRecipe toEntity converts tags to comma-separated string`() {
        val recipe = FilmSimulationRecipe(
            id = "user_recipe_1",
            name = "My Recipe",
            description = "Custom recipe",
            filmSimulation = "Classic Chrome",
            grain = "Off",
            colorChrome = "Weak",
            colorChromeBlue = "Off",
            whiteBalance = "Auto",
            wbShiftR = 0,
            wbShiftB = 0,
            highlights = -1,
            shadows = 0,
            color = -1,
            sharpness = 0,
            noiseReduction = 0,
            isoMin = 160,
            isoMax = 3200,
            tags = listOf("street", "documentary"),
            isUserSaved = true
        )

        val entity = recipe.toEntity()

        assertEquals("street,documentary", entity.tags)
        assertEquals(true, entity.isUserSaved)
        assertEquals(false, entity.isBuiltIn)
        assertEquals("user_recipe_1", entity.id)
    }

    @Test
    fun `RecipeEntity toDomain with empty tags string returns empty list`() {
        val entity = RecipeEntity(
            id = "r1", name = "N", description = "D", filmSimulation = "F",
            grain = "Off", colorChrome = "Off", colorChromeBlue = "Off",
            whiteBalance = "Auto", wbShiftR = 0, wbShiftB = 0,
            highlights = 0, shadows = 0, color = 0, sharpness = 0, noiseReduction = 0,
            isoMin = 160, isoMax = 3200, tags = "", isBuiltIn = true, isUserSaved = false
        )

        val domain = entity.toDomain()

        assertEquals(0, domain.tags.size)
    }
}
