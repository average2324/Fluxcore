package com.luminadigitale.fluxcore.core.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AngleMathTest {
    @Test
    fun normalizeRadians_wrapsNegativeAndPositiveValues() {
        val wrappedNegative = AngleMath.normalizeRadians(-0.5f)
        val wrappedLarge = AngleMath.normalizeRadians(AngleMath.TWO_PI * 5f + 0.5f)

        assertTrue(wrappedNegative in 0f..AngleMath.TWO_PI)
        assertEquals(0.5f, wrappedLarge, 0.0001f)
    }
}
