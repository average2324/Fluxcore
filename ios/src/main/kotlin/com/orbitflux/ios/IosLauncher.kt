package com.orbitflux.ios

import com.badlogic.gdx.backends.iosrobovm.IOSApplication
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration
import com.orbitflux.core.CommercePlatform
import com.orbitflux.core.GameDependencies
import com.orbitflux.core.HexagonGame
import org.robovm.apple.foundation.NSAutoreleasePool
import org.robovm.apple.uikit.UIApplication

class IosLauncher : IOSApplication.Delegate() {
    private val premiumPurchaseService = IosPremiumPurchaseService()

    override fun createApplication(): IOSApplication {
        val config =
            IOSApplicationConfiguration().apply {
                orientationPortrait = true
                orientationLandscape = false
                useAccelerometer = false
                useCompass = false
            }
        return IOSApplication(
            HexagonGame(
                dependencies =
                    GameDependencies(
                        premiumPurchaseService = premiumPurchaseService,
                        commercePlatform = CommercePlatform.APP_STORE,
                        adsEnabled = false,
                    ),
            ),
            config,
        )
    }
}

fun main(args: Array<String>) {
    val pool = NSAutoreleasePool()
    try {
        UIApplication.main(args, UIApplication::class.java, IosLauncher::class.java)
    } finally {
        pool.close()
    }
}
