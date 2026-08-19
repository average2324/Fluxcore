package com.luminadigitale.fluxcore.core.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap

/**
 * Original, procedurally drawn ship art for the hangar/store. The 11 ships are deliberately
 * different archetypes — interceptor, delta shuttle, classic finned rocket, forward-swept
 * fighter, X-wing, spread-wing hawk, dagger, flying-wing manta, split-tail cruiser, heavy
 * capital ship and layered flagship — each drawn nose-up as a solid silhouette, then side-lit
 * with a metallic gradient, outlined, and finished with a canopy and a tail thruster.
 *
 * Everything is generated in code; no bundled ship images, so the app stays free of shared
 * third-party raster assets.
 */
object ShipArt {
    const val WIDTH = 128
    const val HEIGHT = 192
    private const val MID_X = 64

    private val corePalette = listOf(
        "2DF8FF", "63A6FF", "79FF63", "FFC34D", "FF6A9D", "7AB0FF",
        "6CFFEA", "C6FF4D", "FF9E6C", "FF8FF1", "9BB8FF"
    ).map { Color.valueOf(it) }

    private val accentPalette = listOf(
        "D8FBFF", "E7EEFF", "D6FFE3", "FFF4D3", "FFDCEB", "E7EEFF",
        "D6FFF9", "F5FFD7", "FFE7DA", "F5DFFF", "EAF1FF"
    ).map { Color.valueOf(it) }

    /** Caller owns the returned Pixmap and must dispose it. */
    fun buildShipPixmap(index: Int): Pixmap {
        val pm = Pixmap(WIDTH, HEIGHT, Pixmap.Format.RGBA8888)
        pm.setColor(0f, 0f, 0f, 0f)
        pm.fill()

        val i = index.coerceIn(0, 10)
        val core = corePalette[i]
        val accent = accentPalette[i]
        val outline = Color(0.04f, 0.07f, 0.13f, 1f)

        pm.setBlending(Pixmap.Blending.None)
        pm.setColor(core)

        fun tri(ax: Int, ay: Int, bx: Int, by: Int, cx: Int, cy: Int) = pm.fillTriangle(ax, ay, bx, by, cx, cy)
        fun quad(ax: Int, ay: Int, bx: Int, by: Int, cx: Int, cy: Int, dx: Int, dy: Int) {
            tri(ax, ay, bx, by, cx, cy)
            tri(ax, ay, cx, cy, dx, dy)
        }
        fun mir(x: Int) = 2 * MID_X - x
        // Symmetric wing/fin plate: 4 points on the right, auto-mirrored to the left.
        fun plate(ax: Int, ay: Int, bx: Int, by: Int, cx: Int, cy: Int, dx: Int, dy: Int) {
            quad(ax, ay, bx, by, cx, cy, dx, dy)
            quad(mir(ax), ay, mir(bx), by, mir(cx), cy, mir(dx), dy)
        }
        // Central fuselage: nose (top) widening to mid, tapering to tail (bottom).
        fun body(noseY: Int, midY: Int, tailY: Int, halfMid: Int, halfTail: Int) {
            tri(MID_X, noseY, MID_X - halfMid, midY, MID_X + halfMid, midY)
            quad(MID_X - halfMid, midY, MID_X + halfMid, midY, MID_X + halfTail, tailY, MID_X - halfTail, tailY)
        }

        when (i) {
            0 -> { // Specter-7 — sleek interceptor
                body(16, 92, 166, 11, 7)
                plate(53, 88, 24, 150, 36, 150, 55, 108)
                tri(MID_X, 174, MID_X - 15, 156, MID_X - 4, 156)
                tri(MID_X, 174, MID_X + 15, 156, MID_X + 4, 156)
            }
            1 -> { // Nova Arc — delta shuttle
                body(16, 58, 168, 13, 12)
                plate(52, 74, 10, 166, 30, 166, 52, 118)
                quad(MID_X - 5, 150, MID_X + 5, 150, MID_X + 4, 182, MID_X - 4, 182) // stabiliser
            }
            2 -> { // Ion Lancer — classic finned rocket
                tri(MID_X, 12, MID_X - 11, 52, MID_X + 11, 52) // nose cone
                quad(MID_X - 11, 52, MID_X + 11, 52, MID_X + 11, 156, MID_X - 11, 156) // cylinder
                plate(MID_X + 9, 120, MID_X + 30, 170, MID_X + 24, 176, MID_X + 9, 156) // side fins
                quad(MID_X - 6, 156, MID_X + 6, 156, MID_X + 8, 176, MID_X - 8, 176) // centre fin base
            }
            3 -> { // Ember Wing — forward-swept fighter
                body(16, 96, 166, 12, 8)
                plate(52, 138, 22, 92, 38, 104, 54, 120)
                tri(MID_X, 172, MID_X - 12, 156, MID_X + 12, 156)
            }
            4 -> { // Vortex Tail — X-wing (four wings) + twin tail
                body(16, 96, 158, 10, 8)
                plate(52, 108, 22, 78, 30, 86, 54, 116) // upper wings
                plate(52, 120, 22, 150, 30, 156, 54, 128) // lower wings
                tri(MID_X - 6, 156, MID_X - 4, 184, MID_X - 12, 176)
                tri(MID_X + 6, 156, MID_X + 4, 184, MID_X + 12, 176)
            }
            5 -> { // Pulse Hawk — wide spread wings, forked tail
                body(16, 88, 156, 11, 8)
                plate(52, 92, 6, 112, 22, 124, 54, 110)
                tri(MID_X - 4, 152, MID_X - 18, 182, MID_X - 8, 176)
                tri(MID_X + 4, 152, MID_X + 18, 182, MID_X + 8, 176)
            }
            6 -> { // Aether Blade — thin dagger
                body(10, 104, 174, 8, 5)
                plate(52, 118, 42, 92, 47, 98, 54, 132)
                tri(MID_X, 180, MID_X - 5, 166, MID_X + 5, 166)
            }
            7 -> { // Zenith Ray — flying-wing manta (no tail)
                tri(MID_X, 22, MID_X - 54, 152, MID_X + 54, 152)
                quad(MID_X - 54, 152, MID_X + 54, 152, MID_X + 18, 172, MID_X - 18, 172)
                tri(MID_X, 44, MID_X - 12, 150, MID_X + 12, 150)
            }
            8 -> { // Rift Runner — angular cruiser, split rear
                quad(MID_X - 14, 60, MID_X + 14, 60, MID_X + 11, 150, MID_X - 11, 150) // slab body
                tri(MID_X, 20, MID_X - 14, 60, MID_X + 14, 60) // wedge nose
                plate(54, 96, 40, 80, 48, 86, 56, 122) // straight wings
                quad(MID_X - 11, 148, MID_X - 1, 148, MID_X - 9, 186, MID_X - 20, 184)
                quad(MID_X + 11, 148, MID_X + 1, 148, MID_X + 9, 186, MID_X + 20, 184)
            }
            9 -> { // Gravity Falcon — heavy capital ship
                quad(MID_X - 22, 66, MID_X + 22, 66, MID_X + 26, 150, MID_X - 26, 150) // wide slab
                tri(MID_X, 26, MID_X - 22, 66, MID_X + 22, 66) // blunt prow
                plate(MID_X + 24, 92, MID_X + 46, 108, MID_X + 46, 126, MID_X + 24, 130) // stubby thick wings
                quad(MID_X - 16, 150, MID_X + 16, 150, MID_X + 20, 176, MID_X - 20, 176) // engine block
            }
            else -> { // Orion Prime — layered flagship
                body(12, 90, 158, 14, 10)
                plate(50, 62, 30, 42, 40, 50, 52, 74) // canards
                plate(56, 104, 18, 148, 36, 154, 58, 122) // main wings
                quad(MID_X - 3, 40, MID_X + 3, 40, MID_X + 5, 150, MID_X - 5, 150) // dorsal ridge
                tri(MID_X - 6, 156, MID_X - 4, 186, MID_X - 14, 180)
                tri(MID_X + 6, 156, MID_X + 4, 186, MID_X + 14, 180)
            }
        }

        // --- Side-lit shading + edge outline (per row: left highlight, right shadow) ---
        val opaque = BooleanArray(WIDTH * HEIGHT)
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                opaque[y * WIDTH + x] = (pm.getPixel(x, y) and 0xFF) > 12
            }
        }
        val shade = Color()
        for (y in 0 until HEIGHT) {
            var left = -1
            var right = -1
            for (x in 0 until WIDTH) {
                if (opaque[y * WIDTH + x]) {
                    if (left < 0) left = x
                    right = x
                }
            }
            if (left < 0) continue
            val span = (right - left).coerceAtLeast(1).toFloat()
            for (x in left..right) {
                if (!opaque[y * WIDTH + x]) continue
                val t = (x - left) / span
                shade.set(core)
                if (t < 0.42f) {
                    shade.lerp(Color.WHITE, (0.42f - t) / 0.42f * 0.5f)
                } else {
                    shade.lerp(outline, (t - 0.42f) / 0.58f * 0.55f)
                }
                val edge = !opaque[y * WIDTH + (x - 1).coerceAtLeast(0)] ||
                    !opaque[y * WIDTH + (x + 1).coerceAtMost(WIDTH - 1)] ||
                    !opaque[(y - 1).coerceAtLeast(0) * WIDTH + x] ||
                    !opaque[(y + 1).coerceAtMost(HEIGHT - 1) * WIDTH + x]
                if (edge) shade.lerp(outline, 0.7f)
                pm.drawPixel(x, y, Color.rgba8888(shade))
            }
        }

        // --- Canopy near the nose ---
        pm.setBlending(Pixmap.Blending.SourceOver)
        val canopyY = when (i) {
            2 -> 72
            7, 9 -> 84
            else -> 68
        }
        pm.setColor(accent.r, accent.g, accent.b, 0.95f)
        pm.fillCircle(MID_X, canopyY, 8)
        pm.setColor(1f, 1f, 1f, 0.85f)
        pm.fillCircle(MID_X - 2, canopyY - 3, 4)
        pm.setColor(1f, 1f, 1f, 0.9f)
        pm.fillCircle(MID_X, 20, 2)

        // --- Tail thruster, anchored to the hull's actual bottom near the centre line ---
        var baseY = 0
        for (x in (MID_X - 8)..(MID_X + 8)) {
            for (y in HEIGHT - 1 downTo 0) {
                if (opaque[y * WIDTH + x]) {
                    if (y > baseY) baseY = y
                    break
                }
            }
        }
        if (baseY in 1 until HEIGHT - 2) {
            pm.setColor(1f, 0.86f, 0.5f, 0.85f)
            pm.fillTriangle(MID_X - 6, baseY - 3, MID_X + 6, baseY - 3, MID_X, baseY + 12)
            pm.setColor(1f, 1f, 1f, 0.9f)
            pm.fillTriangle(MID_X - 3, baseY - 2, MID_X + 3, baseY - 2, MID_X, baseY + 6)
        }

        return pm
    }
}
