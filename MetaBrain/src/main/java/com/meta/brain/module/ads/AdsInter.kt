package com.meta.brain.module.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.meta.brain.module.base.MetaBrainApp
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager
import com.meta.brain.module.utils.Utility

class AdsInter(val preload: Boolean = false) {

    companion object {
        private const val TAG = "[AdsInter]"
    }

    private var isLoading: Boolean = false
    private var inter: InterstitialAd? = null

    private var currentUnit: String = ""
    private var loadAction: AdEvent? = null


    fun loadInter(context: Context, adUnit: String, event: AdEvent?) {
        currentUnit = adUnit
        loadAction = event

        if (!canUseAds(context) || inter != null || !AdsController.isDuration()) {
            event?.onLoaded()
            return
        }

        if (isLoading) return
        isLoading = true

        logDebug("Load inter: $adUnit")
        FirebaseManager.sendLog("inter_call", null)

        InterstitialAd.load(
            context,
            adUnit,
            AdRequest.Builder().build(),
            loadCallback(event)
        )
    }


    fun showInter(activity: Activity, event: AdEvent?) {
        if (!canUseAds(activity) || !AdsController.isDuration()) {
            event?.onComplete()
            return
        }

        val ad = inter ?: run {
            event?.onComplete()
            FirebaseManager.sendLog("inter_not_avail", null)
            preloadIfNeeded(activity)
            return
        }

        FirebaseManager.sendLog("inter_show", null)
        ad.fullScreenContentCallback = fullscreenCallback(activity, event)
        ad.show(activity)
    }

    private fun canUseAds(context: Context): Boolean {
        if (!FirebaseManager.rc.useAds) return false
        if (DataManager.user.removeAds) return false
        if (FirebaseManager.rc.checkBot && Utility.isBot(context)) return false
        return true
    }

    private fun loadCallback(
        event: AdEvent?
    ) = object : InterstitialAdLoadCallback() {

        override fun onAdLoaded(ad: InterstitialAd) {
            ad.onPaidEventListener = paidListener()
            inter = ad
            isLoading = false

            event?.onLoaded()
            FirebaseManager.sendLog("inter_loaded", null)
            logDebug("Inter loaded")
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            inter = null
            isLoading = false

            event?.onLoadFail()
            FirebaseManager.sendLog("inter_load_fail", null)
            logError(error)
        }
    }

    private fun fullscreenCallback(
        activity: Activity,
        event: AdEvent?
    ) = object : FullScreenContentCallback() {

        override fun onAdDismissedFullScreenContent() {
            AdsController.timeLastInter = System.currentTimeMillis()
            inter = null

            FirebaseManager.sendLog("inter_success", null)
            event?.onComplete()
            preloadIfNeeded(activity)
        }

        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
            inter = null
            FirebaseManager.sendLog("inter_show_fail", null)
            preloadIfNeeded(activity)
        }
    }

    private fun paidListener() = OnPaidEventListener { value ->
        AdsController.logAdRevenue(value, inter?.responseInfo)
    }

    private fun preloadIfNeeded(context: Context) {
        if (!preload) return
        loadInter(context, currentUnit, loadAction)
    }

    private fun logDebug(msg: String) {
        if (MetaBrainApp.debug) Log.d(TAG, msg)
    }

    private fun logError(error: LoadAdError) {
        if (!MetaBrainApp.debug) return
        Log.d(TAG, "Load failed: ${error.code} - ${error.message}")
    }



}