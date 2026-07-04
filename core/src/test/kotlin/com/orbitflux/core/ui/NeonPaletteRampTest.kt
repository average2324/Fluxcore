package com.orbitflux.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NeonPaletteRampTest {
    @Test
    fun paletteStaysInSpaceMissionTriadAcrossCampaign() {
        val early = NeonPaletteRamp.forLevel(1)
        val late = NeonPaletteRamp.forLevel(60)

        assertTrue(early.obstacle.r > early.obstacle.b)
        assertTrue(late.obstacle.r > late.obstacle.b)
        assertTrue(early.background.b > early.background.r)
        assertTrue(late.background.b > late.background.r)
        assertTrue(early.uiAccent.a == 1f && late.uiAccent.a == 1f)
    }

    @Test
    fun tierChangesEveryTenLevels() {
        assertEquals(1, NeonPaletteRamp.tier(1))
        assertEquals(1, NeonPaletteRamp.tier(10))
        assertEquals(2, NeonPaletteRamp.tier(11))
        assertEquals(3, NeonPaletteRamp.tier(25))
        assertEquals(6, NeonPaletteRamp.tier(60))
    }

    @Test
    fun paletteIsDeterministicForLevel() {
        val a = NeonPaletteRamp.forLevel(34)
        val b = NeonPaletteRamp.forLevel(34)

        assertEquals(a.background, b.background)
        assertEquals(a.obstacle, b.obstacle)
        assertEquals(a.uiAccent, b.uiAccent)
    }
}
