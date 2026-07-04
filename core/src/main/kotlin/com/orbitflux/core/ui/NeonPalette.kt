package com.orbitflux.core.ui

import com.badlogic.gdx.graphics.Color

data class NeonPalette(
    val tierName: String,
    val background: Color,
    val phaseBackground: Color,
    val obstacle: Color,
    val obstacleHeavy: Color,
    val obstacleWide: Color,
    val grid: Color,
    val needle: Color,
    val needlePhase: Color,
    val uiAccent: Color
)

object NeonPaletteRamp {
    private data class Anchor(
        val level: Int,
        val tierName: String,
        val background: Color,
        val obstacle: Color,
        val accent: Color
    )

    private val anchors: List<Anchor> = listOf(
        Anchor(1, "Dock Departure", Color(0.008f, 0.02f, 0.05f, 1f), Color(0.12f, 0.16f, 0.2f, 1f), Color(0.24f, 0.86f, 1f, 1f)),
        Anchor(11, "Asteroid Belt", Color(0.01f, 0.022f, 0.055f, 1f), Color(0.14f, 0.18f, 0.23f, 1f), Color(0.28f, 0.9f, 1f, 1f)),
        Anchor(21, "Lagrange Drift", Color(0.012f, 0.024f, 0.06f, 1f), Color(0.15f, 0.2f, 0.25f, 1f), Color(0.32f, 0.95f, 0.97f, 1f)),
        Anchor(31, "Deep Orbit", Color(0.013f, 0.026f, 0.064f, 1f), Color(0.16f, 0.21f, 0.27f, 1f), Color(0.38f, 0.98f, 0.92f, 1f)),
        Anchor(41, "Fracture Zone", Color(0.014f, 0.028f, 0.068f, 1f), Color(0.17f, 0.22f, 0.29f, 1f), Color(0.95f, 0.66f, 0.28f, 1f)),
        Anchor(51, "Command Burn", Color(0.015f, 0.03f, 0.072f, 1f), Color(0.18f, 0.23f, 0.31f, 1f), Color(1f, 0.55f, 0.24f, 1f)),
        Anchor(61, "War Horizon", Color(0.04f, 0.04f, 0.05f, 1f), Color(0.24f, 0.25f, 0.27f, 1f), Color(0.98f, 0.48f, 0.28f, 1f)),
        Anchor(71, "Bio Defense", Color(0.03f, 0.012f, 0.06f, 1f), Color(0.21f, 0.1f, 0.28f, 1f), Color(0.54f, 0.94f, 0.76f, 1f)),
        Anchor(81, "Nova Ambush", Color(0.01f, 0.018f, 0.052f, 1f), Color(0.18f, 0.24f, 0.34f, 1f), Color(0.44f, 0.86f, 1f, 1f)),
        Anchor(91, "Kitchen Rush", Color(0.5f, 0.44f, 0.36f, 1f), Color(0.78f, 0.62f, 0.4f, 1f), Color(0.98f, 0.8f, 0.5f, 1f))
    )

    private val maxLevel = 100
    private val cachedPalettes: List<NeonPalette> = (1..maxLevel).map { createPalette(it) }

    fun forLevel(level: Int): NeonPalette {
        val clamped = level.coerceIn(1, maxLevel)
        return cachedPalettes[clamped - 1]
    }

    fun tier(level: Int): Int {
        return ((level.coerceIn(1, maxLevel) - 1) / 10) + 1
    }

    private fun createPalette(level: Int): NeonPalette {
        val clampedLevel = level.coerceIn(1, maxLevel)
        val blockIndex = ((clampedLevel - 1) / 10).coerceIn(0, anchors.lastIndex)
        val blockAnchor = anchors[blockIndex]
        val blockProgress = ((clampedLevel - blockAnchor.level).toFloat() / 9f).coerceIn(0f, 1f)
        val blockDrift = -0.016f + blockProgress * 0.032f

        // Keep the block identity stable for 10 levels, only add a subtle intra-block drift.
        val background = shade(blockAnchor.background, blockDrift * 0.75f)
        val obstacleBase = shade(blockAnchor.obstacle, blockDrift * 0.62f)
        val accentBase = shade(blockAnchor.accent, blockDrift)
        val metallicDark = Color(0.08f, 0.12f, 0.17f, 1f)
        val obstacle = lerp(obstacleBase, metallicDark, 0.3f).also {
            if (it.r <= it.b) {
                it.r = (it.b + 0.02f).coerceAtMost(1f)
            }
        }
        val accent = lerp(accentBase, Color(0.9f, 1f, 1f, 1f), 0.1f)
        val danger = lerp(Color(1f, 0.4f, 0.2f, 1f), Color(1f, 0.64f, 0.24f, 1f), (tier(clampedLevel) - 1f) / 6f)

        return NeonPalette(
            tierName = blockAnchor.tierName,
            background = background,
            phaseBackground = Color(0.05f, 0.03f, 0.09f, 1f),
            obstacle = obstacle,
            obstacleHeavy = lerp(obstacle, Color(0.05f, 0.07f, 0.11f, 1f), 0.28f),
            obstacleWide = lerp(danger, Color(1f, 0.78f, 0.42f, 1f), 0.22f),
            grid = lerp(background, accent, 0.36f),
            needle = lerp(accent, Color.WHITE, 0.76f),
            needlePhase = lerp(accent, Color(1f, 0.86f, 0.54f, 1f), 0.46f),
            uiAccent = lerp(accent, Color(0.74f, 1f, 0.92f, 1f), 0.24f)
        )
    }

    private fun shade(base: Color, delta: Float): Color {
        return Color(
            (base.r + delta).coerceIn(0f, 1f),
            (base.g + delta).coerceIn(0f, 1f),
            (base.b + delta).coerceIn(0f, 1f),
            1f
        )
    }

    private fun boundsFor(level: Int): Pair<Anchor, Anchor> {
        if (level <= anchors.first().level) {
            return anchors.first() to anchors.first()
        }
        if (level >= anchors.last().level) {
            return anchors.last() to anchors.last()
        }

        for (index in 0 until anchors.lastIndex) {
            val lower = anchors[index]
            val upper = anchors[index + 1]
            if (level in lower.level..upper.level) {
                return lower to upper
            }
        }

        return anchors.last() to anchors.last()
    }

    private fun lerp(start: Color, end: Color, t: Float): Color {
        val p = t.coerceIn(0f, 1f)
        return Color(
            start.r + (end.r - start.r) * p,
            start.g + (end.g - start.g) * p,
            start.b + (end.b - start.b) * p,
            1f
        )
    }
}
