package com.luminadigitale.fluxcore.core

import com.luminadigitale.fluxcore.core.ads.RewardedLifeService
import com.luminadigitale.fluxcore.core.ads.InterstitialAdService
import com.luminadigitale.fluxcore.core.ads.BannerAdService
import com.luminadigitale.fluxcore.core.ads.UnavailableBannerAdService
import com.luminadigitale.fluxcore.core.ads.UnavailableInterstitialAdService
import com.luminadigitale.fluxcore.core.ads.UnavailableRewardedLifeService
import com.luminadigitale.fluxcore.core.lives.EpochSecondsProvider
import com.luminadigitale.fluxcore.core.lives.SystemEpochSecondsProvider
import com.luminadigitale.fluxcore.core.premium.PremiumPurchaseService
import com.luminadigitale.fluxcore.core.premium.UnavailablePremiumPurchaseService

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
