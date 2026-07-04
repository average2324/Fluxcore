package com.orbitflux.core

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.orbitflux.core.ui.GameScreen

class HexagonGame(
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
