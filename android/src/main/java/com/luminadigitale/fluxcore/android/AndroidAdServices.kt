package com.luminadigitale.fluxcore.android

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.luminadigitale.fluxcore.core.ads.BannerAdService
import com.luminadigitale.fluxcore.core.ads.InterstitialAdService
import com.luminadigitale.fluxcore.core.ads.RewardedLifeResult
import com.luminadigitale.fluxcore.core.ads.RewardedLifeService

class AndroidAdServices(
    private val activity: Activity
) : RewardedLifeService, InterstitialAdService, BannerAdService {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerContainer: FrameLayout? = null
    private var bannerView: AdView? = null
    private var rewardedAd: RewardedAd? = null
    private var adsEnabled = false
    private var desiredBannerVisible = false

    fun attachBannerContainer(container: FrameLayout) {
        runOnMain {
            bannerContainer = container
            if (bannerView == null) {
                bannerView = AdView(activity).apply {
                    adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                    setAdSize(AdSize.BANNER)
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    }
                }
                container.addView(bannerView)
            }
            container.visibility = View.GONE
            if (adsEnabled) {
                loadRewarded()
                if (desiredBannerVisible) {
                    container.visibility = View.VISIBLE
                    bannerView?.loadAd(AdRequest.Builder().build())
                }
            }
        }
    }

    fun setAdsEnabled(enabled: Boolean) {
        runOnMain {
            adsEnabled = enabled
            if (!enabled) {
                rewardedAd = null
                bannerContainer?.visibility = View.GONE
                return@runOnMain
            }
            loadRewarded()
            val container = bannerContainer ?: return@runOnMain
            if (desiredBannerVisible) {
                container.visibility = View.VISIBLE
                bannerView?.loadAd(AdRequest.Builder().build())
            } else {
                container.visibility = View.GONE
            }
        }
    }

    override fun setBannerVisible(visible: Boolean) {
        runOnMain {
            desiredBannerVisible = visible
            val container = bannerContainer ?: return@runOnMain
            if (!adsEnabled) {
                container.visibility = View.GONE
                return@runOnMain
            }
            container.visibility = if (desiredBannerVisible) View.VISIBLE else View.GONE
            if (desiredBannerVisible) {
                bannerView?.loadAd(AdRequest.Builder().build())
            }
        }
    }

    override fun isInterstitialAvailable(): Boolean = false

    override fun showInterstitial(onDismissed: () -> Unit) {
        runOnMain {
            onDismissed()
        }
    }

    override fun isRewardAvailable(): Boolean = adsEnabled && rewardedAd != null

    override fun requestLifeReward(onResult: (RewardedLifeResult) -> Unit) {
        runOnMain {
            if (!adsEnabled) {
                onResult(RewardedLifeResult.Failed("Ads consent unavailable"))
                return@runOnMain
            }
            val ad = rewardedAd
            if (ad == null) {
                loadRewarded()
                onResult(RewardedLifeResult.Failed("Rewarded ad unavailable"))
                return@runOnMain
            }
            rewardedAd = null
            var rewarded = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    if (!rewarded) {
                        onResult(RewardedLifeResult.Failed("Reward not earned"))
                    }
                    loadRewarded()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    loadRewarded()
                    onResult(RewardedLifeResult.Failed(adError.message))
                }
            }
            ad.show(activity) { _: RewardItem ->
                rewarded = true
                onResult(RewardedLifeResult.Granted)
            }
        }
    }

    fun destroy() {
        runOnMain {
            bannerView?.destroy()
            bannerView = null
            bannerContainer = null
            rewardedAd = null
        }
    }

    private fun loadRewarded() {
        if (!adsEnabled) {
            return
        }
        RewardedAd.load(
            activity,
            BuildConfig.ADMOB_REWARDED_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}
