package com.luminadigitale.fluxcore.core.engine

import com.badlogic.gdx.math.MathUtils
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollisionEngineTest {
    private val collider = CollisionEngine.PlayerCollider(
        radius = 0.72f,
        halfArcWidthRad = 0.04f,
        radialTolerance = 0.02f
    )

    @Test
    fun collides_whenPlayerIsInBlockedSectorAndRadiusOverlaps() {
        val obstacle = Obstacle(
            sectorCount = 6,
            radius = 0.73f,
            speed = 0.4f,
            thickness = 0.05f,
            gapStartSector = 2,
            gapSectorCount = 1
        )

        val angleInBlockedSector = 0.2f
        assertTrue(CollisionEngine.collides(angleInBlockedSector, collider, obstacle))
    }

    @Test
    fun doesNotCollide_whenPlayerIsInGap() {
        val obstacle = Obstacle(
            sectorCount = 6,
            radius = 0.73f,
            speed = 0.4f,
            thickness = 0.05f,
            gapStartSector = 0,
            gapSectorCount = 1
        )

        val angleInGap = 0.2f
        assertFalse(CollisionEngine.collides(angleInGap, collider, obstacle))
    }

    @Test
    fun doesNotCollide_whenRadiusDoesNotOverlap() {
        val obstacle = Obstacle(
            sectorCount = 6,
            radius = 1.0f,
            speed = 0.4f,
            thickness = 0.05f,
            gapStartSector = 0,
            gapSectorCount = 1
        )

        assertFalse(CollisionEngine.collides(0.2f, collider, obstacle))
    }

    @Test
    fun rotationMovesTheGap() {
        val obstacle = Obstacle(
            sectorCount = 6,
            radius = 0.73f,
            speed = 0.4f,
            thickness = 0.05f,
            gapStartSector = 0,
            gapSectorCount = 1,
            rotationRad = MathUtils.PI / 3f
        )

        assertTrue(CollisionEngine.collides(0.2f, collider, obstacle))
        assertFalse(CollisionEngine.collides(MathUtils.PI / 3f + 0.2f, collider, obstacle))
    }
}
