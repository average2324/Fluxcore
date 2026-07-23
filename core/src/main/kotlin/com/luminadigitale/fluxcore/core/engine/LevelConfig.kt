package com.luminadigitale.fluxcore.core.engine

data class LevelConfig(
    val index: Int,
    val sectorCount: Int,
    val gapSectorCount: Int,
    val featureTier: Int,
    val baseObstacleSpeed: Float,
    val obstacleThickness: Float,
    val spawnIntervalMinSeconds: Float,
    val spawnIntervalMaxSeconds: Float,
    val targetSurvivalSeconds: Float,
    val needleAngularSpeedRad: Float,
    val minRadialGapBetweenRings: Float,
    val targetRadialGapBetweenRings: Float,
    val minReadableThreatGapSeconds: Float,
    val maxConcurrentThreatDensity: Int,
    val laneShiftSeverity: Int
)
