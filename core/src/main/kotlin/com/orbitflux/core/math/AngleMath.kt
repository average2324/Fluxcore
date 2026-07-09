package com.luminadigitale.fluxcore.core.math

import kotlin.math.PI

object AngleMath {
    const val PI_F: Float = PI.toFloat()
    const val TWO_PI: Float = (PI * 2.0).toFloat()

    fun normalizeRadians(angle: Float): Float {
        var normalized = angle % TWO_PI
        if (normalized < 0f) {
            normalized += TWO_PI
        }
        if (normalized >= TWO_PI) {
            normalized -= TWO_PI
        }
        return normalized
    }
}
