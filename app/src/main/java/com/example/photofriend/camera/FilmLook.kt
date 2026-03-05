package com.example.photofriend.camera

import android.graphics.ColorMatrix

/**
 * Converts a map of setting-name → value (e.g. "Film Simulation" → "Velvia/Vivid")
 * into a [ViewfinderEffectParams] that can be applied to the live preview.
 *
 * Uses Android's [ColorMatrix] for computation; the resulting float array is
 * passed to Compose's ColorMatrix in the UI layer.
 */
object FilmLook {

    fun build(settings: Map<String, String>): ViewfinderEffectParams {
        val filmSim    = settings["Film Simulation"]     ?: "Provia/Standard"
        val grain      = settings["Grain Effect"]        ?: "Off"
        val chrome     = settings["Color Chrome Effect"] ?: "Off"
        val chromeBlue = settings["Color Chrome FX Blue"] ?: "Off"
        val wb         = settings["White Balance"]       ?: "Auto"
        val highlights = settings["Highlight Tone"].toSignedInt()
        val shadows    = settings["Shadow Tone"].toSignedInt()
        val color      = settings["Color"].toSignedInt()      // -4..+4 saturation
        val clarity    = settings["Clarity"].toSignedInt()    // -5..+5

        val m = ColorMatrix()

        // 1. Film simulation base look
        m.postConcat(filmSimMatrix(filmSim))

        // 2. User saturation (Color setting): each step ≈ ±12 %
        if (color != 0) m.postConcat(satMatrix(1f + color * 0.12f))

        // 3. Tone adjustments (each step ≈ ±8 brightness + contrast boost)
        if (highlights != 0 || shadows != 0) m.postConcat(toneMatrix(highlights, shadows))

        // 4. White balance tint (colour-temperature presets + Kelvin values)
        m.postConcat(wbMatrix(wb))

        // 5. Color Chrome Effect — approximate as mild saturation + deep-colour boost
        if (chrome != "Off") {
            val boost = if (chrome == "Strong") 0.15f else 0.08f
            m.postConcat(satMatrix(1f + boost))
        }

        // 6. Color Chrome FX Blue — gentle blue-channel lift
        if (chromeBlue != "Off") {
            val lift = if (chromeBlue == "Strong") 0.12f else 0.06f
            m.postConcat(channelScaleMatrix(r = 1f, g = 1f, b = 1f + lift))
        }

        // 7. Clarity — mid-tone contrast; each step ≈ ±2.5 % contrast
        if (clarity != 0) m.postConcat(contrastMatrix(1f + clarity * 0.025f))

        val (grainAmount, grainSizePx) = grainParams(grain)
        return ViewfinderEffectParams(
            colorMatrixValues = m.array.clone(),
            grainAmount       = grainAmount,
            grainSizePx       = grainSizePx
        )
    }

    // ── Film simulation look definitions ────────────────────────────────────

    private fun filmSimMatrix(name: String): ColorMatrix = when (name) {
        "Velvia/Vivid" -> combined(
            sat = 1.55f, contrast = 1.08f,
            r = 1.04f, g = 0.95f, b = 1.05f
        )
        "Astia/Soft" -> combined(
            sat = 0.88f, contrast = 0.96f,
            r = 1.04f, g = 1.0f, b = 0.97f, brightness = 4f
        )
        "Classic Chrome" -> combined(
            sat = 0.62f, contrast = 1.06f,
            r = 0.97f, g = 0.97f, b = 1.07f, brightness = -4f
        )
        "Pro Neg. Hi" -> combined(
            sat = 1.06f, contrast = 1.07f,
            r = 1.02f, g = 1.0f, b = 0.97f
        )
        "Pro Neg. Std" -> combined(
            sat = 0.92f, contrast = 0.99f,
            r = 1.02f, g = 1.0f, b = 0.96f, brightness = 2f
        )
        "Classic Neg." -> combined(
            sat = 0.68f, contrast = 1.10f,
            r = 0.97f, g = 1.03f, b = 0.94f
        )
        "Nostalgic Neg." -> combined(
            sat = 0.72f, contrast = 0.95f,
            r = 1.10f, g = 0.97f, b = 0.84f, brightness = 5f
        )
        "Eterna/Cinema" -> combined(
            sat = 0.52f, contrast = 0.88f,
            r = 0.97f, g = 0.98f, b = 1.01f, brightness = 8f
        )
        "Eterna Bleach Bypass" -> combined(
            sat = 0.28f, contrast = 1.14f,
            r = 0.97f, g = 0.97f, b = 1.02f
        )
        "Acros", "Acros+R", "Acros+G", "Acros+Ye" -> acrosMatrix(name)
        "Monochrome", "Monochrome+R", "Monochrome+G", "Monochrome+Ye" -> monoMatrix(name)
        "Sepia" -> sepiaMatrix()
        "Reala Ace" -> combined(sat = 0.97f, r = 1.0f, g = 1.0f, b = 1.03f)
        else -> ColorMatrix() // Provia/Standard = identity
    }

    // ── Matrix helpers ───────────────────────────────────────────────────────

    /** Builds a [ColorMatrix] from common photographic adjustments. */
    private fun combined(
        sat: Float = 1f,
        contrast: Float = 1f,
        r: Float = 1f,
        g: Float = 1f,
        b: Float = 1f,
        brightness: Float = 0f
    ): ColorMatrix {
        val m = ColorMatrix()
        m.setSaturation(sat)
        if (contrast != 1f) m.postConcat(contrastMatrix(contrast))
        m.postConcat(channelScaleMatrix(r, g, b, brightness))
        return m
    }

    /** Scales each channel equally, centred at mid-grey (128). */
    private fun contrastMatrix(contrast: Float): ColorMatrix {
        val t = 128f * (1f - contrast)
        return ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, t,
            0f, contrast, 0f, 0f, t,
            0f, 0f, contrast, 0f, t,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    /** Per-channel scale with an optional uniform brightness offset. */
    private fun channelScaleMatrix(
        r: Float, g: Float, b: Float, brightness: Float = 0f
    ) = ColorMatrix(floatArrayOf(
        r, 0f, 0f, 0f, brightness,
        0f, g, 0f, 0f, brightness,
        0f, 0f, b, 0f, brightness,
        0f, 0f, 0f, 1f, 0f
    ))

    private fun satMatrix(sat: Float) = ColorMatrix().also { it.setSaturation(sat) }

    /**
     * Approximate tone-curve: highlights push bright areas, shadows lift/crush darks.
     * Uses a linear brightness + contrast model (good enough for live preview).
     */
    private fun toneMatrix(highlights: Int, shadows: Int): ColorMatrix {
        // highlights: pushes bright areas up/down (each step ≈ ±5 brightness, ±3% contrast)
        // shadows: lifts/crushes dark areas (each step ≈ ±8 brightness offset)
        val brightness = shadows * 8f + highlights * 5f
        val contrast   = 1f + (highlights - shadows) * 0.03f
        val offset     = 128f * (1f - contrast) + brightness
        return ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, offset,
            0f, contrast, 0f, 0f, offset,
            0f, 0f, contrast, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    /**
     * Colour-temperature → RGB multipliers.
     * Named hardware presets (Auto, Daylight, Incandescent, Fluorescent) are handled
     * by Camera2 AWB and return identity here to avoid double-correction.
     */
    private fun wbMatrix(wb: String): ColorMatrix {
        val (r, g, b) = when (wb) {
            "2500K"         -> Triple(1.22f, 0.98f, 0.68f)
            "3200K"         -> Triple(1.15f, 0.99f, 0.78f)
            "4000K"         -> Triple(1.08f, 1.00f, 0.88f)
            "5000K"         -> Triple(1.00f, 1.00f, 1.00f)
            "6500K"         -> Triple(0.96f, 1.00f, 1.10f)
            "10000K"        -> Triple(0.86f, 0.98f, 1.22f)
            "Shade"         -> Triple(0.98f, 1.00f, 1.08f)
            "Underwater"    -> Triple(1.10f, 1.02f, 0.88f)
            else            -> Triple(1.00f, 1.00f, 1.00f) // Auto, Daylight, Fluorescent, Incandescent
        }
        // Normalise so brightest channel stays at 1 (avoids clipping)
        val maxC = maxOf(r, g, b)
        return channelScaleMatrix(r / maxC, g / maxC, b / maxC)
    }

    // ── B&W / special simulations ────────────────────────────────────────────

    private fun luminanceWeights(name: String): Triple<Float, Float, Float> = when {
        name.endsWith("+R")  -> Triple(0.50f, 0.35f, 0.15f) // red filter
        name.endsWith("+G")  -> Triple(0.20f, 0.65f, 0.15f) // green filter
        name.endsWith("+Ye") -> Triple(0.40f, 0.50f, 0.10f) // yellow filter
        else                 -> Triple(0.299f, 0.587f, 0.114f)
    }

    private fun monoMatrix(name: String): ColorMatrix {
        val (lr, lg, lb) = luminanceWeights(name)
        return ColorMatrix(floatArrayOf(
            lr, lg, lb, 0f, 0f,
            lr, lg, lb, 0f, 0f,
            lr, lg, lb, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    private fun acrosMatrix(name: String): ColorMatrix {
        val m = monoMatrix(name)
        m.postConcat(contrastMatrix(1.12f)) // Acros has higher micro-contrast
        return m
    }

    private fun sepiaMatrix(): ColorMatrix {
        val m = monoMatrix("Monochrome")
        m.postConcat(channelScaleMatrix(r = 1.12f, g = 0.92f, b = 0.72f))
        return m
    }

    // ── Grain ────────────────────────────────────────────────────────────────

    /** Returns (grainAmount 0..1, grainSizePx). */
    private fun grainParams(grain: String): Pair<Float, Float> = when (grain) {
        "Weak Small"   -> Pair(0.055f, 1.2f)
        "Weak Large"   -> Pair(0.055f, 2.8f)
        "Strong Small" -> Pair(0.13f,  1.2f)
        "Strong Large" -> Pair(0.13f,  2.8f)
        else           -> Pair(0f, 0f)
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    private fun String?.toSignedInt(): Int =
        this?.removePrefix("+")?.trim()?.toIntOrNull() ?: 0
}
