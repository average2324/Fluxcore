package com.luminadigitale.fluxcore.core.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap

/**
 * Original, procedurally drawn ship art for the hangar/store. Each of the 11 ships is drawn
 * nose-up as a distinct solid silhouette — different hull, wing planform and tail — then
 * side-lit with a vertical metallic gradient and a clean edge outline, plus a canopy and
 * engine glow. Everything is generated in code; no bundled ship images, so the app stays
 * free of shared third-party raster assets.
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

        // Filled triangle + quad helpers, with an X-mirror so ships stay symmetric.
        fun tri(ax: Int, ay: Int, bx: Int, by: Int, cx: Int, cy: Int) = pm.fillTriangle(ax, ay, bx, by, cx, cy)
        fun quad(ax: Int, ay: Int, bx: Int, by: Int, cx: Int, cy: Int, dx: Int, dy: Int) {
            tri(ax, ay, bx, by, cx, cy)
            tri(ax, ay, cx, cy, dx, dy)
        }
        fun m(x: Int) = 2 * MID_X - x
        // Wing as a 4-point plate on the right side + mirror. (rx=root x, tx=tip x, y's top/bottom.)
        fun wing(rxTop: Int, ryTop: Int, txTop: Int, tyTop: Int, txBot: Int, tyBot: Int, rxBot: Int, ryBot: Int) {
            quad(rxTop, ryTop, txTop, tyTop, txBot, tyBot, rxBot, ryBot)
            quad(m(rxTop), ryTop, m(txTop), tyTop, m(txBot), tyBot, m(rxBot), ryBot)
        }
        // Central body spanning nose (top) to tail (bottom); halfTop/halfMid/halfTail = half widths.
        fun body(noseY: Int, midY: Int, tailY: Int, halfMid: Int, halfTail: Int) {
            tri(MID_X, noseY, MID_X - halfMid, midY, MID_X + halfMid, midY)
            quad(MID_X - halfMid, midY, MID_X + halfMid, midY, MID_X + halfTail, tailY, MID_X - halfTail, tailY)
        }

        when (i) {
            0 -> { // Specter-7 — sleek interceptor, thin swept wings, small V-tail
                body(16, 96, 168, 12, 8)
                wing(54, 92, 22, 150, 34, 150, 56, 108)
                tri(MID_X, 176, MID_X - 16, 158, MID_X - 4, 158)
                tri(MID_X, 176, MID_X + 16, 158, MID_X + 4, 158)
            }
            1 -> { // Nova Arc — broad delta wing
                body(14, 60, 170, 10, 14)
                wing(54, 70, 8, 168, 30, 168, 54, 120)
                quad(MID_X - 14, 168, MID_X + 14, 168, MID_X + 8, 182, MID_X - 8, 182)
            }
            2 -> { // Ion Lancer — long needle, tiny wings, long spike
                tri(MID_X, 8, MID_X - 6, 60, MID_X + 6, 60)
                body(52, 92, 166, 8, 6)
                wing(50, 104, 30, 138, 40, 138, 52, 118)
                tri(MID_X, 176, MID_X - 8, 160, MID_X + 8, 160)
            }
            3 -> { // Ember Wing — thick forward-swept wings
                body(16, 92, 168, 13, 9)
                wing(52, 132, 20, 96, 40, 108, 54, 116)
                wing(54, 150, 26, 176, 44, 176, 56, 150)
            }
            4 -> { // Vortex Tail — medium wings, wide curved twin tail
                body(16, 90, 156, 12, 10)
                wing(52, 96, 26, 140, 38, 144, 54, 112)
                quad(MID_X - 10, 150, MID_X - 4, 150, MID_X - 30, 184, MID_X - 22, 178)
                quad(MID_X + 10, 150, MID_X + 4, 150, MID_X + 30, 184, MID_X + 22, 178)
            }
            5 -> { // Pulse Hawk — broad spread wings, forked tail
                body(16, 84, 158, 11, 9)
                wing(52, 78, 6, 118, 26, 128, 54, 104)
                tri(MID_X - 4, 156, MID_X - 20, 184, MID_X - 8, 176)
                tri(MID_X + 4, 156, MID_X + 20, 184, MID_X + 8, 176)
                quad(MID_X - 4, 150, MID_X + 4, 150, MID_X + 3, 176, MID_X - 3, 176)
            }
            6 -> { // Aether Blade — thin sharp blade, minimal straight wings
                body(10, 100, 172, 8, 5)
                wing(52, 118, 40, 96, 46, 100, 54, 128)
                tri(MID_X, 178, MID_X - 6, 162, MID_X + 6, 162)
            }
            7 -> { // Zenith Ray — wide flat manta, wings merge with body, no tail
                tri(MID_X, 20, MID_X - 52, 150, MID_X + 52, 150)
                quad(MID_X - 52, 150, MID_X + 52, 150, MID_X + 20, 172, MID_X - 20, 172)
                tri(MID_X, 40, MID_X - 12, 150, MID_X + 12, 150)
            }
            8 -> { // Rift Runner — chunky angular hull, straight wings, split tail
                body(16, 88, 150, 14, 11)
                wing(54, 92, 40, 78, 48, 84, 56, 118)
                quad(MID_X - 12, 148, MID_X - 2, 148, MID_X - 10, 184, MID_X - 20, 184)
                quad(MID_X + 12, 148, MID_X + 2, 148, MID_X + 10, 184, MID_X + 20, 184)
            }
            9 -> { // Gravity Falcon — heavy wide cruiser, thick short wings, big single tail
                body(18, 84, 158, 18, 14)
                wing(60, 92, 36, 120, 48, 128, 62, 116)
                quad(MID_X - 8, 150, MID_X + 8, 150, MID_X + 14, 186, MID_X - 14, 186)
            }
            else -> { // Orion Prime — flagship: canards + main wings + twin tail
                body(14, 88, 160, 15, 11)
                wing(52, 60, 34, 40, 42, 48, 54, 74) // canards
                wing(58, 104, 20, 150, 38, 154, 60, 124) // main wings
                quad(MID_X - 11, 156, MID_X - 2, 156, MID_X - 8, 188, MID_X - 18, 186)
                quad(MID_X + 11, 156, MID_X + 2, 156, MID_X + 8, 188, MID_X + 18, 186)
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

        // --- Canopy near the nose, engine glow at the tail, nose spark ---
        pm.setBlending(Pixmap.Blending.SourceOver)
        pm.setColor(accent.r, accent.g, accent.b, 0.95f)
        pm.fillCircle(MID_X, 70, 9)
        pm.setColor(1f, 1f, 1f, 0.85f)
        pm.fillCircle(MID_X - 2, 66, 4)
        for (k in 0..3) {
            val r = 8 - k
            if (r <= 0) continue
            pm.setColor(accent.r, accent.g, accent.b, 0.5f - k * 0.11f)
            pm.fillCircle(MID_X, 168 + k * 3, r)
        }
        pm.setColor(1f, 1f, 1f, 0.9f)
        pm.fillCircle(MID_X, 20, 2)

        return pm
    }
}
