package com.orbitflux.core.engine

data class PhaseGateStatus(
    val active: Boolean,
    val directionMultiplier: Int,
    val secondsToStateChange: Float
)
