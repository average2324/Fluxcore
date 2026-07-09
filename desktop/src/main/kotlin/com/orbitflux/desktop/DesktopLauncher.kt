package com.luminadigitale.fluxcore.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.luminadigitale.fluxcore.core.CommercePlatform
import com.luminadigitale.fluxcore.core.GameDependencies
import com.luminadigitale.fluxcore.core.HexagonGame
import com.luminadigitale.fluxcore.core.ads.SimulatedInterstitialAdService
import com.luminadigitale.fluxcore.core.ads.SimulatedRewardedLifeService
import com.luminadigitale.fluxcore.core.ads.UnavailableBannerAdService
import com.luminadigitale.fluxcore.core.premium.SimulatedPremiumPurchaseService

fun main(args: Array<String>) {
    val smokeSeconds = args
        .firstOrNull { it.startsWith("--smoke-seconds=") }
        ?.substringAfter("=")
        ?.toFloatOrNull()
    val simulationMode = args.any { it == "--simulate" || it == "--sim" }
    val appStorePreview = args.any { it == "--app-store-preview" || it == "--ios-preview" }
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
    val premiumPrice = args
        .firstOrNull { it.startsWith("--premium-price=") }
        ?.substringAfter("=")
        ?: if (appStorePreview) "79,99" else "$4.99"

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
                premiumPurchaseService = SimulatedPremiumPurchaseService(
                    description = "One-time premium unlock with unlimited lives.",
                    priceLabel = premiumPrice
                ),
                commercePlatform = if (appStorePreview) CommercePlatform.APP_STORE else CommercePlatform.GENERIC,
                adsEnabled = !appStorePreview,
                simulationModeEnabled = simulationMode,
                simulationStartLevel = simulationLevelIndex
            )
        ),
        config
    )
}
