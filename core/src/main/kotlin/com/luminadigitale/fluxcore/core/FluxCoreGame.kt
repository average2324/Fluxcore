package com.luminadigitale.fluxcore.core

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.luminadigitale.fluxcore.core.ui.GameScreen

class FluxCoreGame(
    private val autoExitSeconds: Float? = null,
    private val dependencies: GameDependencies = GameDependencies()
) : Game() {
    private var elapsedSeconds = 0f

    override fun create() {
        setScreen(GameScreen(dependencies))
    }

    override fun render() {
        super.render()

        val limit = autoExitSeconds ?: return
        elapsedSeconds += Gdx.graphics.deltaTime
        if (elapsedSeconds >= limit) {
            Gdx.app.exit()
        }
    }
}
