package com.luminadigitale.fluxcore.core

import kotlin.test.Test
import kotlin.test.assertEquals

class BuildSanityTest {
    @Test
    fun packageNameIsStable() {
        assertEquals("com.luminadigitale.fluxcore.core", HexagonGame::class.java.`package`.name)
    }
}
