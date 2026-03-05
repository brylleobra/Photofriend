package com.example.photofriend.data.local.seed

import com.example.photofriend.data.local.db.entity.CameraModelEntity
import com.example.photofriend.data.local.db.entity.CameraSettingEntity

object CameraSeeds {

    val cameras = listOf(
        CameraModelEntity(
            id = "fujifilm_xt30iii",
            brand = "Fujifilm",
            name = "X-T30 III",
            sensorSize = "APS-C",
            megapixels = 40,
            filmSimulationCount = 20,
            releaseYear = 2024
        ),
        CameraModelEntity(
            id = "fujifilm_xt5",
            brand = "Fujifilm",
            name = "X-T5",
            sensorSize = "APS-C",
            megapixels = 40,
            filmSimulationCount = 20,
            releaseYear = 2022
        ),
        CameraModelEntity(
            id = "fujifilm_xs20",
            brand = "Fujifilm",
            name = "X-S20",
            sensorSize = "APS-C",
            megapixels = 26,
            filmSimulationCount = 20,
            releaseYear = 2023
        ),
        CameraModelEntity(
            id = "fujifilm_x100vi",
            brand = "Fujifilm",
            name = "X100VI",
            sensorSize = "APS-C",
            megapixels = 40,
            filmSimulationCount = 20,
            releaseYear = 2024
        ),
        CameraModelEntity(
            id = "fujifilm_xpro3",
            brand = "Fujifilm",
            name = "X-Pro3",
            sensorSize = "APS-C",
            megapixels = 26,
            filmSimulationCount = 18,
            releaseYear = 2019
        )
    )

    private fun filmSimOptions() = listOf(
        "Provia/Standard", "Velvia/Vivid", "Astia/Soft",
        "Classic Chrome", "Pro Neg. Hi", "Pro Neg. Std",
        "Classic Neg.", "Nostalgic Neg.", "Eterna/Cinema",
        "Eterna Bleach Bypass", "Acros", "Acros+R", "Acros+G", "Acros+Ye",
        "Monochrome", "Monochrome+R", "Monochrome+G", "Monochrome+Ye",
        "Sepia", "Reala Ace"
    ).joinToString("|")

    private fun grainOptions() = listOf(
        "Off", "Weak Small", "Weak Large", "Strong Small", "Strong Large"
    ).joinToString("|")

    private fun colorChromeOptions() = listOf("Off", "Weak", "Strong").joinToString("|")

    private fun wbOptions() = listOf(
        "Auto", "Auto White Priority", "Auto Ambience Priority",
        "Daylight", "Shade", "Fluorescent 1", "Fluorescent 2", "Fluorescent 3",
        "Incandescent", "Underwater", "Custom 1", "Custom 2", "Custom 3",
        "2500K", "3200K", "4000K", "5000K", "6500K", "10000K"
    ).joinToString("|")

    private fun toneOptions() = listOf("-2", "-1", "0", "+1", "+2", "+3", "+4").joinToString("|")

    private fun colorOptions() = listOf(
        "-4", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4"
    ).joinToString("|")

    private fun sharpOptions() = listOf(
        "-4", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4"
    ).joinToString("|")

    private fun nrOptions() = listOf(
        "-4", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4"
    ).joinToString("|")

    private fun clarityOptions() = listOf(
        "-5", "-4", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4", "+5"
    ).joinToString("|")

    private fun drOptions() = listOf("Auto", "DR100", "DR200", "DR400").joinToString("|")

    private fun isoOptions() = listOf(
        "Auto", "160", "200", "320", "400", "640", "800", "1600",
        "3200", "6400", "12800", "25600", "51200"
    ).joinToString("|")

    val xt30iiiSettings = listOf(
        CameraSettingEntity(
            id = "xt30iii_film_sim",
            cameraId = "fujifilm_xt30iii",
            category = "COLOR",
            name = "Film Simulation",
            description = "Emulates the color and tonal characteristics of Fujifilm film stocks",
            options = filmSimOptions(),
            defaultValue = "Provia/Standard"
        ),
        CameraSettingEntity(
            id = "xt30iii_grain",
            cameraId = "fujifilm_xt30iii",
            category = "COLOR",
            name = "Grain Effect",
            description = "Adds film grain texture to images",
            options = grainOptions(),
            defaultValue = "Off"
        ),
        CameraSettingEntity(
            id = "xt30iii_color_chrome",
            cameraId = "fujifilm_xt30iii",
            category = "COLOR",
            name = "Color Chrome Effect",
            description = "Enhances color depth and saturation in richly colored areas",
            options = colorChromeOptions(),
            defaultValue = "Off"
        ),
        CameraSettingEntity(
            id = "xt30iii_color_chrome_blue",
            cameraId = "fujifilm_xt30iii",
            category = "COLOR",
            name = "Color Chrome FX Blue",
            description = "Adds gradation to blue tones, preventing over-saturation",
            options = colorChromeOptions(),
            defaultValue = "Off"
        ),
        CameraSettingEntity(
            id = "xt30iii_wb",
            cameraId = "fujifilm_xt30iii",
            category = "COLOR",
            name = "White Balance",
            description = "Adjusts color temperature to match the light source",
            options = wbOptions(),
            defaultValue = "Auto"
        ),
        CameraSettingEntity(
            id = "xt30iii_highlight",
            cameraId = "fujifilm_xt30iii",
            category = "COLOR",
            name = "Highlight Tone",
            description = "Controls the brightness of highlight areas",
            options = toneOptions(),
            defaultValue = "0"
        ),
        CameraSettingEntity(
            id = "xt30iii_shadow",
            cameraId = "fujifilm_xt30iii",
            category = "COLOR",
            name = "Shadow Tone",
            description = "Controls the brightness of shadow areas",
            options = toneOptions(),
            defaultValue = "0"
        ),
        CameraSettingEntity(
            id = "xt30iii_color",
            cameraId = "fujifilm_xt30iii",
            category = "COLOR",
            name = "Color",
            description = "Adjusts overall color saturation",
            options = colorOptions(),
            defaultValue = "0"
        ),
        CameraSettingEntity(
            id = "xt30iii_sharpness",
            cameraId = "fujifilm_xt30iii",
            category = "OUTPUT",
            name = "Sharpness",
            description = "Controls the sharpness of image edges",
            options = sharpOptions(),
            defaultValue = "0"
        ),
        CameraSettingEntity(
            id = "xt30iii_nr",
            cameraId = "fujifilm_xt30iii",
            category = "OUTPUT",
            name = "Noise Reduction",
            description = "Reduces digital noise in images",
            options = nrOptions(),
            defaultValue = "0"
        ),
        CameraSettingEntity(
            id = "xt30iii_clarity",
            cameraId = "fujifilm_xt30iii",
            category = "OUTPUT",
            name = "Clarity",
            description = "Controls mid-tone contrast for perceived sharpness",
            options = clarityOptions(),
            defaultValue = "0"
        ),
        CameraSettingEntity(
            id = "xt30iii_dr",
            cameraId = "fujifilm_xt30iii",
            category = "EXPOSURE",
            name = "Dynamic Range",
            description = "Expands the tonal range captured in highlights",
            options = drOptions(),
            defaultValue = "DR100"
        ),
        CameraSettingEntity(
            id = "xt30iii_iso",
            cameraId = "fujifilm_xt30iii",
            category = "EXPOSURE",
            name = "ISO",
            description = "Controls sensor sensitivity to light",
            options = isoOptions(),
            defaultValue = "Auto"
        )
    )
}
