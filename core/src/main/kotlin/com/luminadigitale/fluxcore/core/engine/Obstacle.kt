package com.luminadigitale.fluxcore.core.engine

data class Obstacle(
    val sectorCount: Int,
    var radius: Float,
    var speed: Float,
    val thickness: Float,
    val gapStartSector: Int,
    val gapSectorCount: Int,
    val patternId: String = "base",
    var rotationRad: Float = 0f,
    val spinRadPerSecond: Float = 0f
)
