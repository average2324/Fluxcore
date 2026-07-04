package com.orbitflux.core

import kotlin.test.Test
import kotlin.test.assertEquals

class BuildSanityTest {
    @Test
    fun packageNameIsStable() {
        assertEquals("com.orbitflux.core", HexagonGame::class.java.`package`.name)
    }
}
