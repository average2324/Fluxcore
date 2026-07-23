package com.luminadigitale.fluxcore.android

import android.app.ActivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.google.android.gms.ads.MobileAds
import com.luminadigitale.fluxcore.core.CommercePlatform
import com.luminadigitale.fluxcore.core.FluxCoreGame
import com.luminadigitale.fluxcore.core.GameDependencies

class AndroidLauncher : AndroidApplication() {
    private lateinit var adServices: AndroidAdServices
    private lateinit var premiumPurchaseService: AndroidPremiumPurchaseService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLogger.readAndClear(this)?.let { dump ->
            Log.e("FluxCoreCrash", "Previous session crash detected:\n$dump")
        }
        CrashLogger.install(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val lowRamDevice = activityManager.isLowRamDevice || activityManager.memoryClass <= 192

        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            numSamples = if (lowRamDevice) 2 else 4
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
            useRotationVectorSensor = false
        }

        adServices = AndroidAdServices(this)
        premiumPurchaseService = AndroidPremiumPurchaseService(this)

        val game = FluxCoreGame(
            dependencies = GameDependencies(
                rewardedLifeService = adServices,
                interstitialAdService = adServices,
                bannerAdService = adServices,
                premiumPurchaseService = premiumPurchaseService,
                commercePlatform = CommercePlatform.GOOGLE_PLAY,
                simulationModeEnabled = BuildConfig.SIMULATION_MODE,
                simulationStartLevel = BuildConfig.SIMULATION_START_LEVEL.takeIf { it >= 0 }
            )
        )
        val gameView = initializeForView(game, config)
        val root = FrameLayout(this)
        root.addView(
            gameView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        val bannerContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM
            }
        }
        root.addView(bannerContainer)
        setContentView(root)
        adServices.attachBannerContainer(bannerContainer)

        ConsentManager(this).gatherConsent { canRequestAds ->
            runOnUiThread {
                if (canRequestAds) {
                    MobileAds.initialize(this)
                } else {
                    Log.w("FluxCoreAds", "Ads disabled because consent is unavailable.")
                }
                adServices.setAdsEnabled(canRequestAds)
            }
        }
    }

    override fun onDestroy() {
        if (this::adServices.isInitialized) {
            adServices.destroy()
        }
        if (this::premiumPurchaseService.isInitialized) {
            premiumPurchaseService.destroy()
        }
        super.onDestroy()
    }
}
