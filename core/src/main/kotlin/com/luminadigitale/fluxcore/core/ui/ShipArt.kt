package com.luminadigitale.fluxcore.core.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap

/**
 * Original, procedurally drawn ship art. Each of the 11 ships is a distinct hull + wing +
 * tail silhouette, drawn as a flat mask and then shaded with a top-lit vertical gradient and
 * a clean edge outline so it reads as a solid vehicle rather than thin placeholder lines.
 *
 * Everything here is generated in code — no bundled ship images — which keeps the app free of
 * shared third-party raster assets. Shared by the in-game store and the desktop preview tool.
 */
object ShipArt {
    const val WIDTH = 192
    const val HEIGHT = 128

    private val corePalette = listOf(
        "2DF8FF", "63EDFF", "79FF63", "FFC34D", "FF6A9D", "7AB0FF",
        "6CFFEA", "E3FF78", "FF9E6C", "FF8FF1", "9BB8FF"
    ).map { Color.valueOf(it) }

    private val accentPalette = listOf(
        "D8FBFF", "FFE0C1", "D6FFE3", "FFF4D3", "FFDCEB", "E7EEFF",
        "D6FFF9", "F5FFD7", "FFE7DA", "F5DFFF", "EAF1FF"
    ).map { Color.valueOf(it) }

    private data class ShipForm(
        val noseX: Int,
        val tailX: Int,
        val bodyHalf: Int,
        val wRootX: Int,
        val wTipX: Int,
        val wSpan: Int,
        val wThick: Int,
        val canardX: Int,
        val canardSpan: Int,
        val twinTail: Boolean,
        val tailFinSpan: Int
    )

    private val shipForms = listOf(
        ShipForm(184, 36, 12, 118, 66, 30, 10, 0, 0, false, 18), // 0 dart interceptor (default)
        ShipForm(176, 40, 13, 128, 44, 46, 8, 0, 0, false, 16), // 1 wide delta
        ShipForm(190, 44, 9, 120, 84, 20, 6, 150, 14, false, 12), // 2 ion lancer (needle + canard)
        ShipForm(178, 40, 12, 96, 132, 34, 9, 0, 0, false, 20), // 3 ember wing (forward-swept)
        ShipForm(172, 34, 14, 132, 70, 26, 12, 0, 0, true, 30), // 4 vortex tail (twin fins)
        ShipForm(174, 44, 11, 120, 72, 44, 7, 0, 0, false, 14), // 5 pulse hawk (broad spread)
        ShipForm(188, 46, 7, 110, 80, 16, 5, 0, 0, false, 22), // 6 aether blade (thin sharp)
        ShipForm(168, 36, 16, 140, 52, 52, 10, 0, 0, false, 12), // 7 zenith ray (wide manta)
        ShipForm(178, 30, 11, 116, 74, 28, 9, 0, 0, true, 24), // 8 rift runner (split tail)
        ShipForm(166, 34, 18, 120, 78, 30, 14, 96, 20, false, 16), // 9 gravity falcon (bulky + canard)
        ShipForm(182, 32, 15, 128, 66, 40, 12, 104, 22, true, 26) // 10 orion prime (flagship)
    )

    /** Caller owns the returned Pixmap and must dispose it. */
    fun buildShipPixmap(index: Int): Pixmap {
        val width = WIDTH
        val height = HEIGHT
        val midY = height / 2
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0f)
        pixmap.fill()

        val core = corePalette[index.coerceIn(0, corePalette.lastIndex)]
        val accent = accentPalette[index.coerceIn(0, accentPalette.lastIndex)]
        val outlineColor = Color(0.04f, 0.07f, 0.13f, 1f)
        val form = shipForms[index.coerceIn(0, shipForms.lastIndex)]

        pixmap.setBlending(Pixmap.Blending.None)
        pixmap.setColor(core)

        // Body: pointed nose tapering to a squared tail, with a slight belly for volume.
        val midBodyX = (form.noseX + form.tailX) / 2
        pixmap.fillTriangle(form.noseX, midY, form.tailX, midY - form.bodyHalf, form.tailX, midY + form.bodyHalf)
        pixmap.fillTriangle(form.noseX, midY, midBodyX, midY - form.bodyHalf - 2, form.tailX, midY - form.bodyHalf)
        pixmap.fillTriangle(form.noseX, midY, midBodyX, midY + form.bodyHalf + 2, form.tailX, midY + form.bodyHalf)

        // Main wings (top + mirrored bottom). tipX > rootX sweeps them forward.
        pixmap.fillTriangle(form.wRootX, midY - 2, form.wTipX, midY - form.wSpan, form.wRootX - form.wThick, midY - 2)
        pixmap.fillTriangle(form.wRootX, midY + 2, form.wTipX, midY + form.wSpan, form.wRootX - form.wThick, midY + 2)

        // Optional forward canards.
        if (form.canardSpan > 0) {
            pixmap.fillTriangle(form.canardX, midY - 2, form.canardX - 14, midY - form.canardSpan, form.canardX - 18, midY - 2)
            pixmap.fillTriangle(form.canardX, midY + 2, form.canardX - 14, midY + form.canardSpan, form.canardX - 18, midY + 2)
        }

        // Tail fins: twin splayed fins or a single stabiliser.
        if (form.twinTail) {
            pixmap.fillTriangle(form.tailX + 18, midY - 3, form.tailX - 6, midY - form.tailFinSpan, form.tailX + 4, midY - 3)
            pixmap.fillTriangle(form.tailX + 18, midY + 3, form.tailX - 6, midY + form.tailFinSpan, form.tailX + 4, midY + 3)
        } else {
            pixmap.fillTriangle(form.tailX + 20, midY, form.tailX - 2, midY - form.tailFinSpan, form.tailX + 6, midY)
            pixmap.fillTriangle(form.tailX + 20, midY, form.tailX - 2, midY + form.tailFinSpan, form.tailX + 6, midY)
        }

        // --- Shading + outline pass (the actual polish) ---
        val opaque = BooleanArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                opaque[y * width + x] = (pixmap.getPixel(x, y) and 0xFF) > 12
            }
        }
        val shade = Color()
        for (x in 0 until width) {
            var top = -1
            var bottom = -1
            for (y in 0 until height) {
                if (opaque[y * width + x]) {
                    if (top < 0) top = y
                    bottom = y
                }
            }
            if (top < 0) continue
            val span = (bottom - top).coerceAtLeast(1).toFloat()
            for (y in top..bottom) {
                if (!opaque[y * width + x]) continue
                val t = (y - top) / span
                shade.set(core)
                if (t < 0.30f) {
                    shade.lerp(Color.WHITE, (0.30f - t) / 0.30f * 0.55f)
                } else {
                    shade.lerp(outlineColor, (t - 0.30f) / 0.70f * 0.5f)
                }
                val edge = !opaque[y * width + (x - 1).coerceAtLeast(0)] ||
                    !opaque[y * width + (x + 1).coerceAtMost(width - 1)] ||
                    !opaque[(y - 1).coerceAtLeast(0) * width + x] ||
                    !opaque[(y + 1).coerceAtMost(height - 1) * width + x]
                if (edge) shade.lerp(outlineColor, 0.72f)
                pixmap.drawPixel(x, y, Color.rgba8888(shade))
            }
        }

        // --- Canopy, engine glow and nose spark, drawn over the shaded hull ---
        pixmap.setBlending(Pixmap.Blending.SourceOver)
        val canopyX = (form.noseX * 0.60f + form.tailX * 0.40f).toInt()
        val canopyR = (form.bodyHalf * 0.55f).toInt().coerceAtLeast(4)
        pixmap.setColor(accent.r, accent.g, accent.b, 0.95f)
        pixmap.fillCircle(canopyX, midY, canopyR)
        pixmap.setColor(1f, 1f, 1f, 0.85f)
        pixmap.fillCircle(canopyX + 2, midY - 1, (canopyR * 0.5f).toInt().coerceAtLeast(2))
        for (k in 0..3) {
            val glowR = (form.bodyHalf * 0.65f).toInt().coerceAtLeast(3) - k
            if (glowR <= 0) continue
            pixmap.setColor(accent.r, accent.g, accent.b, 0.5f - k * 0.11f)
            pixmap.fillCircle(form.tailX - k * 3, midY, glowR)
        }
        pixmap.setColor(1f, 1f, 1f, 0.9f)
        pixmap.fillCircle(form.noseX - 4, midY, 2)

        return pixmap
    }
}
