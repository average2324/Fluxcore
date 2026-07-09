package com.luminadigitale.fluxcore.core.engine

enum class PatternFamily {
    SINGLE_THREAT,
    DOUBLE_PHRASE,
    DELAYED_FOLLOW_UP,
    ALTERNATING_PRESSURE,
    SPIRAL_PRESSURE,
    FAKE_OUT_SHIFT
}

data class PatternTemplate(
    val id: String,
    val family: PatternFamily,
    val unlockTier: Int,
    val sectorOffset: Int,
    val gapOverride: Int? = null,
    val speedMultiplierMin: Float,
    val speedMultiplierMax: Float,
    val thicknessMultiplierMin: Float,
    val thicknessMultiplierMax: Float,
    val spawnIntervalMultiplierMin: Float,
    val spawnIntervalMultiplierMax: Float
)
