package com.meta.brain.module.ads

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.google.android.gms.ads.*
import com.meta.brain.module.base.MetaBrainApp
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager
import com.meta.brain.module.utils.Utility

enum class BannerSizeType {
    BANNER,
    LARGE_BANNER,
    FULL_BANNER,
    MEDIUM_RECTANGLE,
    ADAPTIVE // Default adaptive banner size
}

class AdsBanner {
    companion object {
        private const val TAG: String = "[AdsBanner]"
    }

    private var adView: AdView? = null

    /**
     * Load banner ad with specified size
     * @param context Context
     * @param adUnit Ad unit ID
     * @param container ViewGroup container
     * @param sizeType Banner size type. If null, uses adaptive banner size
     */
    fun loadBanner(context: Context, adUnit:String, container: ViewGroup, sizeType: BannerSizeType? = null) {
        if(FirebaseManager.rc.useAds && !DataManager.user.removeAds) {
            if (MetaBrainApp.debug) {
                Log.d(TAG, "Banner Ad call, id: $adUnit, sizeType: $sizeType")
            }
            FirebaseManager.sendLog("banner_call",null)
            val adView = AdView(context)
            adView.adUnitId = adUnit
            
            val adSize = getAdSize(context, sizeType)
            adView.setAdSize(adSize)

            this.adView = adView

            container.removeAllViews()
            container.addView(adView)

            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)

            adView.adListener = object : AdListener() {
                override fun onAdClicked() {
                    // Code to be executed when the user clicks on an ad.
                    if (MetaBrainApp.debug) {
                        Log.d(TAG, "Banner Ad click.")
                    }
                }

                override fun onAdClosed() {
                    // Code to be executed when the user is about to return
                    // to the app after tapping on an ad.
                    if (MetaBrainApp.debug) {
                        Log.d(TAG, "Banner Ad closed.")
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    if(MetaBrainApp.debug) {
                        Log.d(TAG, "Banner Ads load fail: " + adError.message)
                    }
                    FirebaseManager.sendLog("banner_load_fail",null)
                }

                override fun onAdImpression() {
                    // Code to be executed when an impression is recorded
                    // for an ad.
                    if (MetaBrainApp.debug) {
                        Log.d(TAG, "Banner Ad impress.")
                    }
                }

                override fun onAdLoaded() {
                    if(MetaBrainApp.debug) {
                        Log.d(TAG, "Banner Ads loaded")
                    }
                    FirebaseManager.sendLog("banner_loaded",null)
                }

                override fun onAdOpened() {
                    // Code to be executed when an ad opens an overlay that
                    // covers the screen.
                    if (MetaBrainApp.debug) {
                        Log.d(TAG, "Banner Ad open cover screen.")
                    }
                }
            }
            adView.onPaidEventListener = OnPaidEventListener { adValue -> AdsController.logAdRevenue(adValue,adView.responseInfo) }
        }
    }

    /**
     * Get AdSize based on sizeType parameter
     * @param context Context for adaptive banner
     * @param sizeType BannerSizeType enum value
     * @return AdSize object
     */
    private fun getAdSize(context: Context, sizeType: BannerSizeType?): AdSize {
        return when (sizeType) {
            BannerSizeType.BANNER -> AdSize.BANNER
            BannerSizeType.LARGE_BANNER -> AdSize.LARGE_BANNER
            BannerSizeType.FULL_BANNER -> AdSize.FULL_BANNER
            BannerSizeType.MEDIUM_RECTANGLE -> AdSize.MEDIUM_RECTANGLE
            BannerSizeType.ADAPTIVE -> AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                context,
                Utility.getAdWidth(context)
            )
            null -> AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                context,
                Utility.getAdWidth(context)
            )
        }
    }

}