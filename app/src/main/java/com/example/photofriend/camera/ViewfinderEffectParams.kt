package com.example.photofriend.camera

data class ViewfinderEffectParams(
    /** 20-element float array matching Android/Compose ColorMatrix row-major format. */
    val colorMatrixValues: FloatArray = IDENTITY.clone(),
    val grainAmount: Float = 0f,   // 0 = off … 0.14 = strong
    val grainSizePx: Float = 1.5f  // radius in dp-pixels
) {
    companion object {
        private val IDENTITY = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        val NONE = ViewfinderEffectParams()
    }

    // FloatArray requires manual equals/hashCode to satisfy data class contract.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ViewfinderEffectParams) return false
        return colorMatrixValues.contentEquals(other.colorMatrixValues)
            && grainAmount == other.grainAmount
            && grainSizePx == other.grainSizePx
    }

    override fun hashCode(): Int {
        var result = colorMatrixValues.contentHashCode()
        result = 31 * result + grainAmount.hashCode()
        result = 31 * result + grainSizePx.hashCode()
        return result
    }
}
