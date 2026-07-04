package com.orbitflux.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.orbitflux.core.GameDependencies
import com.orbitflux.core.HexagonGame
import com.orbitflux.core.ads.SimulatedInterstitialAdService
import com.orbitflux.core.ads.SimulatedRewardedLifeService
import com.orbitflux.core.ads.UnavailableBannerAdService
import com.orbitflux.core.premium.SimulatedPremiumPurchaseService

fun main(args: Array<String>) {
    val smokeSeconds = args
        .firstOrNull { it.startsWith("--smoke-seconds=") }
        ?.substringAfter("=")
        ?.toFloatOrNull()
    val simulationMode = args.any { it == "--simulate" || it == "--sim" }
    val simulationLevelIndex = args
        .firstOrNull { it.startsWith("--sim-level=") }
        ?.substringAfter("=")
        ?.toIntOrNull()
        ?.minus(1)
    val windowWidth = args
        .firstOrNull { it.startsWith("--width=") }
        ?.substringAfter("=")
        ?.toIntOrNull()
        ?: 720
    val windowHeight = args
        .firstOrNull { it.startsWith("--height=") }
        ?.substringAfter("=")
        ?.toIntOrNull()
        ?: 1280

    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("FluxCore")
        setWindowedMode(windowWidth, windowHeight)
        setBackBufferConfig(8, 8, 8, 8, 16, 0, 4)
        setForegroundFPS(60)
        useVsync(true)
        setHdpiMode(HdpiMode.Pixels)
        setResizable(true)
    }

    Lwjgl3Application(
        HexagonGame(
            autoExitSeconds = smokeSeconds,
            dependencies = GameDependencies(
                rewardedLifeService = SimulatedRewardedLifeService(),
                interstitialAdService = SimulatedInterstitialAdService(),
                bannerAdService = UnavailableBannerAdService,
                premiumPurchaseService = SimulatedPremiumPurchaseService(),
                simulationModeEnabled = simulationMode,
                simulationStartLevel = simulationLevelIndex
            )
        ),
        config
    )
}
