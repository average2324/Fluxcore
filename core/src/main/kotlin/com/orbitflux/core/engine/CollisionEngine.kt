package com.orbitflux.core.engine

import com.orbitflux.core.math.AngleMath

object CollisionEngine {
    data class PlayerCollider(
        val radius: Float,
        val halfArcWidthRad: Float,
        val radialTolerance: Float
    )

    fun collides(
        playerAngleRad: Float,
        collider: PlayerCollider,
        obstacle: Obstacle
    ): Boolean {
        val obstacleOuter = obstacle.radius
        val obstacleInner = obstacle.radius - obstacle.thickness
        val wingArc = collider.halfArcWidthRad * 1.58f
        val samples = listOf(
            // Nose and canopy
            SamplePoint(collider.radius + collider.radialTolerance * 0.92f, AngleMath.normalizeRadians(playerAngleRad)),
            SamplePoint(
                collider.radius + collider.radialTolerance * 0.68f,
                AngleMath.normalizeRadians(playerAngleRad - wingArc * 0.24f)
            ),
            SamplePoint(
                collider.radius + collider.radialTolerance * 0.68f,
                AngleMath.normalizeRadians(playerAngleRad + wingArc * 0.24f)
            ),
            SamplePoint(
                collider.radius + collider.radialTolerance * 0.4f,
                AngleMath.normalizeRadians(playerAngleRad - wingArc * 0.14f)
            ),
            SamplePoint(
                collider.radius + collider.radialTolerance * 0.4f,
                AngleMath.normalizeRadians(playerAngleRad + wingArc * 0.14f)
            ),

            // Body sides
            SamplePoint(
                collider.radius + collider.radialTolerance * 0.12f,
                AngleMath.normalizeRadians(playerAngleRad - wingArc * 0.46f)
            ),
            SamplePoint(
                collider.radius + collider.radialTolerance * 0.12f,
                AngleMath.normalizeRadians(playerAngleRad + wingArc * 0.46f)
            ),
            SamplePoint(
                collider.radius - collider.radialTolerance * 0.04f,
                AngleMath.normalizeRadians(playerAngleRad - wingArc * 0.72f)
            ),
            SamplePoint(
                collider.radius - collider.radialTolerance * 0.04f,
                AngleMath.normalizeRadians(playerAngleRad + wingArc * 0.72f)
            ),

            // Wing tips
            SamplePoint(
                collider.radius - collider.radialTolerance * 0.24f,
                AngleMath.normalizeRadians(playerAngleRad - wingArc * 0.94f)
            ),
            SamplePoint(
                collider.radius - collider.radialTolerance * 0.24f,
                AngleMath.normalizeRadians(playerAngleRad + wingArc * 0.94f)
            ),

            // Tail and fins
            SamplePoint(collider.radius - collider.radialTolerance * 0.92f, AngleMath.normalizeRadians(playerAngleRad)),
            SamplePoint(
                collider.radius - collider.radialTolerance * 0.72f,
                AngleMath.normalizeRadians(playerAngleRad - wingArc * 0.36f)
            ),
            SamplePoint(
                collider.radius - collider.radialTolerance * 0.72f,
                AngleMath.normalizeRadians(playerAngleRad + wingArc * 0.36f)
            )
        )

        for (sample in samples) {
            if (sample.radius < obstacleInner || sample.radius > obstacleOuter) {
                continue
            }
            if (isBlockedByObstacle(sample.angle, obstacle)) {
                return true
            }
        }
        return false
    }

    private data class SamplePoint(
        val radius: Float,
        val angle: Float
    )

    fun isSectorInGap(
        sectorIndex: Int,
        sectorCount: Int,
        gapStartSector: Int,
        gapSectorCount: Int
    ): Boolean {
        if (gapSectorCount >= sectorCount) {
            return true
        }

        val normalizedSector = ((sectorIndex % sectorCount) + sectorCount) % sectorCount
        val normalizedGapStart = ((gapStartSector % sectorCount) + sectorCount) % sectorCount
        val relative = ((normalizedSector - normalizedGapStart) + sectorCount) % sectorCount
        return relative < gapSectorCount
    }

    private fun isBlockedByObstacle(angleRad: Float, obstacle: Obstacle): Boolean {
        val sectorAngle = AngleMath.TWO_PI / obstacle.sectorCount.toFloat()
        val localAngle = AngleMath.normalizeRadians(angleRad - obstacle.rotationRad)
        var sectorIndex = (localAngle / sectorAngle).toInt()
        if (sectorIndex >= obstacle.sectorCount) {
            sectorIndex = obstacle.sectorCount - 1
        }

        val inGap = isSectorInGap(
            sectorIndex = sectorIndex,
            sectorCount = obstacle.sectorCount,
            gapStartSector = obstacle.gapStartSector,
            gapSectorCount = obstacle.gapSectorCount
        )

        return !inGap
    }
}
