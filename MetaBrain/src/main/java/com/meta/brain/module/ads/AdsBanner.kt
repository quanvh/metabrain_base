package com.meta.brain.module.ads

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.google.android.gms.ads.*
import com.meta.brain.module.base.MetaBrainApp
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager
import com.meta.brain.module.utils.Utility

class AdsBanner {
    companion object {
        private const val TAG: String = "[AdsBanner]"
    }

    private var adView: AdView? = null

    fun loadBanner(context: Context, adUnit:String, container: ViewGroup) {
        if(FirebaseManager.rc.useAds && !DataManager.user.removeAds) {
            FirebaseManager.sendLog("banner_call",null)
            val adView = AdView(context)
            adView.adUnitId = adUnit
            adView.setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    context,
                    Utility.getAdWidth(context)
                )
            )

            this.adView = adView

            container.removeAllViews()
            container.addView(adView)

            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)

            adView.adListener = object : AdListener() {
                override fun onAdClicked() {
                    // Code to be executed when the user clicks on an ad.
                }

                override fun onAdClosed() {
                    // Code to be executed when the user is about to return
                    // to the app after tapping on an ad.
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
                }
            }
        }
    }

}