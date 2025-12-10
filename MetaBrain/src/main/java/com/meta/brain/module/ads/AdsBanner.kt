package com.meta.brain.module.ads

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.google.android.gms.ads.*
import com.meta.brain.module.base.MetaBrainApp
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager
import com.meta.brain.module.utils.Utility
import com.meta.brain.module.utils.debugLog
import com.meta.brain.module.utils.gone
import com.meta.brain.module.utils.invisible
import com.meta.brain.module.utils.visible

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
    fun loadBanner(
        context: Context,
        adUnit: String,
        container: ViewGroup,
        sizeType: BannerSizeType? = null
    ) {

        if (!FirebaseManager.rc.useAds || DataManager.user.removeAds) {
            container.removeAllViews()
            container.gone()
            return
        }

        debugLog(TAG, "Banner call, id=$adUnit, sizeType=$sizeType")
        FirebaseManager.sendLog("banner_call", null)

        // Create the new banner but don’t remove the old one
        val newBanner = AdView(context).apply {
            adUnitId = adUnit
            setAdSize(getAdSize(context, sizeType))
        }

        //  Replace when the new banner is loaded.
        newBanner.adListener = object : AdListener() {

            override fun onAdLoaded() {
                debugLog(TAG, "Banner loaded")
                FirebaseManager.sendLog("banner_loaded", null)

                // delete banner old and change banner new
                adView?.destroy()
                container.removeAllViews()
                container.addView(newBanner)

                // update
                adView = newBanner
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                debugLog(TAG, "Banner load fail: ${error.message}")
                FirebaseManager.sendLog("banner_load_fail", null)
            }

            override fun onAdClicked() = debugLog(TAG, "Banner clicked")
            override fun onAdImpression() = debugLog(TAG, "Banner impression")
            override fun onAdOpened() = debugLog(TAG, "Banner open overlay")
            override fun onAdClosed() = debugLog(TAG, "Banner closed")
        }

        newBanner.onPaidEventListener =
            OnPaidEventListener { adValue ->
                AdsController.logAdRevenue(adValue, newBanner.responseInfo)
            }

        // Load ad (cái cũ vẫn hiển thị trong lúc tải)
        newBanner.loadAd(AdRequest.Builder().build())
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