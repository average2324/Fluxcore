package com.orbitflux.core

import com.orbitflux.core.ads.RewardedLifeService
import com.orbitflux.core.ads.InterstitialAdService
import com.orbitflux.core.ads.BannerAdService
import com.orbitflux.core.ads.UnavailableBannerAdService
import com.orbitflux.core.ads.UnavailableInterstitialAdService
import com.orbitflux.core.ads.UnavailableRewardedLifeService
import com.orbitflux.core.lives.EpochSecondsProvider
import com.orbitflux.core.lives.SystemEpochSecondsProvider
import com.orbitflux.core.premium.PremiumPurchaseService
import com.orbitflux.core.premium.UnavailablePremiumPurchaseService

enum class CommercePlatform {
    GENERIC,
    GOOGLE_PLAY,
    APP_STORE
}

data class GameDependencies(
    val epochSecondsProvider: EpochSecondsProvider = SystemEpochSecondsProvider,
    val rewardedLifeService: RewardedLifeService = UnavailableRewardedLifeService,
    val interstitialAdService: InterstitialAdService = UnavailableInterstitialAdService,
    val bannerAdService: BannerAdService = UnavailableBannerAdService,
    val premiumPurchaseService: PremiumPurchaseService = UnavailablePremiumPurchaseService,
    val commercePlatform: CommercePlatform = CommercePlatform.GENERIC,
    val adsEnabled: Boolean = true,
    val campaignSeed: Long = 0x6A09E667F3BCC909L,
    val simulationModeEnabled: Boolean = false,
    val simulationStartLevel: Int? = null
)
