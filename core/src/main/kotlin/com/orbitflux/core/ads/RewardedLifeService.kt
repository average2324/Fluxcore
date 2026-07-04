package com.orbitflux.core.ads

sealed class RewardedLifeResult {
    data object Granted : RewardedLifeResult()
    data class Failed(val reason: String) : RewardedLifeResult()
}

interface RewardedLifeService {
    fun isRewardAvailable(): Boolean
    fun requestLifeReward(onResult: (RewardedLifeResult) -> Unit)
}

interface InterstitialAdService {
    fun isInterstitialAvailable(): Boolean
    fun showInterstitial(onDismissed: () -> Unit)
}

interface BannerAdService {
    fun setBannerVisible(visible: Boolean)
}

object UnavailableRewardedLifeService : RewardedLifeService {
    override fun isRewardAvailable(): Boolean = false

    override fun requestLifeReward(onResult: (RewardedLifeResult) -> Unit) {
        onResult(RewardedLifeResult.Failed("Rewarded ad unavailable"))
    }
}

class SimulatedRewardedLifeService : RewardedLifeService {
    override fun isRewardAvailable(): Boolean = true

    override fun requestLifeReward(onResult: (RewardedLifeResult) -> Unit) {
        onResult(RewardedLifeResult.Granted)
    }
}

object UnavailableInterstitialAdService : InterstitialAdService {
    override fun isInterstitialAvailable(): Boolean = false

    override fun showInterstitial(onDismissed: () -> Unit) {
        onDismissed()
    }
}

object UnavailableBannerAdService : BannerAdService {
    override fun setBannerVisible(visible: Boolean) = Unit
}

class SimulatedInterstitialAdService : InterstitialAdService {
    override fun isInterstitialAvailable(): Boolean = true

    override fun showInterstitial(onDismissed: () -> Unit) {
        onDismissed()
    }
}
