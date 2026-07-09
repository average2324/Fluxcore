package com.luminadigitale.fluxcore.core.engine

import kotlin.test.Test
import kotlin.test.assertTrue

class LevelPatternVarietyTest {
    @Test
    fun firstFiveLevels_onlyUseBeginnerPatternPool() {
        val levels = LevelCatalog.create(60)
        val beginnerPool = setOf("clean_arc", "narrow_gate", "wide_ring", "drift_gap")

        for (levelIndex in 0 until 5) {
            val generator = PatternGenerator(levels[levelIndex], 303030L + levelIndex)
            repeat(20) {
                val obstacle = generator.nextObstacle(1.2f, 1f)
                assertTrue(
                    obstacle.patternId in beginnerPool,
                    "level=${levelIndex + 1} pattern=${obstacle.patternId}"
                )
            }
        }
    }

    @Test
    fun consecutiveLevels_produceDifferentPatternSignatures() {
        val levels = LevelCatalog.create(60)
        val signatures = ArrayList<String>(20)

        for (levelIndex in 0 until 20) {
            val generator = PatternGenerator(levels[levelIndex], 505050L)
            val ids = buildList {
                repeat(10) {
                    add(generator.nextObstacle(1.2f, 1.08f).patternId)
                }
            }
            signatures += ids.joinToString(",")
        }

        for (windowStart in 0..15 step 5) {
            val uniqueInWindow = signatures.subList(windowStart, windowStart + 5).toSet().size
            assertTrue(
                uniqueInWindow >= 2,
                "expected >=2 unique signatures in levels ${windowStart + 1}-${windowStart + 5}, found $uniqueInWindow"
            )
        }
    }

    @Test
    fun fluxCoreSignaturePatterns_unlockAfterOpeningBlock() {
        val levels = LevelCatalog.create(60)

        val levelTwentyFourPatterns = generatedPatternIds(levels[23])
        val levelFortyOnePatterns = generatedPatternIds(levels[40])

        assertTrue("flux_mirror" in levelTwentyFourPatterns, "level 24 patterns=$levelTwentyFourPatterns")
        assertTrue("core_surge" in levelFortyOnePatterns, "level 41 patterns=$levelFortyOnePatterns")
    }

    private fun generatedPatternIds(level: LevelConfig): Set<String> {
        val ids = mutableSetOf<String>()
        for (seed in 1L..80L) {
            val generator = PatternGenerator(level, 700000L + seed)
            repeat(8) {
                ids += generator.nextSpawn(1.2f, 1.08f).obstacle.patternId
            }
        }
        return ids
    }
}
