package com.luminadigitale.fluxcore.desktop

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.luminadigitale.fluxcore.core.ui.ShipArt

/**
 * Renders all 11 procedural ships into one PNG grid so the art can be reviewed on desktop
 * without opening the store UI. Usage: gradle :desktop:shipPreview  (writes ship_preview.png).
 */
fun main(args: Array<String>) {
    val outPath = args.firstOrNull { !it.startsWith("--") } ?: "ship_preview.png"
    val scale = 2 // upscale so the shading is easy to inspect
    val config = Lwjgl3ApplicationConfiguration().apply {
        setWindowedMode(200, 200)
        setTitle("FluxCore Ship Preview")
        disableAudio(true)
    }

    Lwjgl3Application(
        object : ApplicationAdapter() {
            override fun create() {
                val cols = 3
                val rows = 4
                val pad = 12
                val cellW = ShipArt.WIDTH * scale
                val cellH = ShipArt.HEIGHT * scale
                val gridW = cols * cellW + (cols + 1) * pad
                val gridH = rows * cellH + (rows + 1) * pad

                val grid = Pixmap(gridW, gridH, Pixmap.Format.RGBA8888)
                grid.setColor(0.05f, 0.06f, 0.11f, 1f)
                grid.fill()
                grid.setBlending(Pixmap.Blending.SourceOver)

                for (i in 0..10) {
                    val src = ShipArt.buildShipPixmap(i)
                    val up = Pixmap(cellW, cellH, Pixmap.Format.RGBA8888)
                    up.setBlending(Pixmap.Blending.None)
                    up.drawPixmap(src, 0, 0, src.width, src.height, 0, 0, cellW, cellH)
                    val col = i % cols
                    val row = i / cols
                    val x = pad + col * (cellW + pad)
                    val y = pad + row * (cellH + pad)
                    // subtle cell backdrop so transparent ships stand out
                    grid.setColor(0.09f, 0.11f, 0.18f, 1f)
                    grid.fillRectangle(x, y, cellW, cellH)
                    grid.drawPixmap(up, x, y)
                    up.dispose()
                    src.dispose()
                }

                val file = Gdx.files.absolute(java.io.File(outPath).absolutePath)
                PixmapIO.writePNG(file, grid)
                grid.dispose()
                Gdx.app.log("ShipPreview", "Wrote ${file.path()} (${gridW}x$gridH)")
                Gdx.app.exit()
            }
        },
        config
    )
}
