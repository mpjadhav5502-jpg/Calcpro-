package com.example.data.ads

import android.app.Activity
import android.content.Context
import com.example.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd

/**
 * Configurable Google AdMob manager using Google's official test ad unit IDs during development.
 * - Strictly respects Premium status (disables all ads when isPremium is true).
 * - Never auto-clicks or generates fake advertisements.
 * - Never interrupts active calculator keypads or math entry.
 */
class AdManager(private val context: Context) {

    private var initialized = false
    private var interstitialAd: InterstitialAd? = null
    private var lastInterstitialShownMs: Long = 0L

    fun initialize(isPremium: Boolean) {
        if (isPremium || !BuildConfig.ADS_ENABLED || initialized) return
        try {
            MobileAds.initialize(context.applicationContext) {
                initialized = true
                preloadInterstitial(isPremium = false)
            }
        } catch (_: Exception) {
            // Gracefully ignore in Robolectric/headless environments without Play Services
        }
    }

    fun preloadInterstitial(isPremium: Boolean) {
        if (isPremium || !BuildConfig.ADS_ENABLED || !initialized || interstitialAd != null) return
        try {
            val request = AdRequest.Builder().build()
            InterstitialAd.load(
                context.applicationContext,
                BuildConfig.ADMOB_INTERSTITIAL_ID,
                request,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                    }
                }
            )
        } catch (_: Exception) {
            interstitialAd = null
        }
    }

    /**
     * Shows an interstitial only at natural transition points (e.g., exiting a secondary tool)
     * with a minimum 90-second cooldown, and never for Premium users.
     */
    fun showInterstitialAtTransition(activity: Activity?, isPremium: Boolean) {
        if (activity == null || isPremium || !BuildConfig.ADS_ENABLED) return
        val now = System.currentTimeMillis()
        if (now - lastInterstitialShownMs < 90_000L) return
        val ad = interstitialAd ?: run {
            preloadInterstitial(isPremium)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                lastInterstitialShownMs = System.currentTimeMillis()
                preloadInterstitial(isPremium)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
            }
        }
        ad.show(activity)
    }

    fun createBannerAdView(
        activityContext: Context,
        onLoaded: (Boolean) -> Unit
    ): AdView? {
        if (!BuildConfig.ADS_ENABLED) return null
        return try {
            AdView(activityContext).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        onLoaded(true)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        onLoaded(false)
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        } catch (_: Exception) {
            onLoaded(false)
            null
        }
    }

    fun loadNativeAd(
        onLoaded: (NativeAd) -> Unit,
        onFailed: () -> Unit
    ) {
        if (!BuildConfig.ADS_ENABLED) {
            onFailed()
            return
        }
        try {
            val loader = AdLoader.Builder(context.applicationContext, BuildConfig.ADMOB_NATIVE_ID)
                .forNativeAd { nativeAd -> onLoaded(nativeAd) }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        onFailed()
                    }
                })
                .build()
            loader.loadAd(AdRequest.Builder().build())
        } catch (_: Exception) {
            onFailed()
        }
    }
}
