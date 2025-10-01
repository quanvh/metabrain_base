package com.meta.brain.module.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.meta.brain.module.base.MetaBrainApp
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager

class AdsInter (val preload: Boolean = false) {

    companion object {
        private const val TAG = "[AdsInter]"
    }
    private var adIsLoading: Boolean = false
    private var inter: InterstitialAd? = null

    private var currentUnit: String = ""

    private var loadAction :AdEvent? = null

    private var timeLastInter: Long = 0
    private var firstStart: Boolean = true

    init {
        timeLastInter = System.currentTimeMillis()
    }

    fun loadInter(context: Context, adUnit:String, onEvent: AdEvent?) {
        loadAction = onEvent
        currentUnit = adUnit

        if(!FirebaseManager.rc.useAds && DataManager.user.removeAds && inter != null){
            onEvent?.onLoaded()
            return
        }

        if (adIsLoading) return

        adIsLoading = true

        FirebaseManager.sendLog("inter_call",null)
        InterstitialAd.load(
            context,
            adUnit,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    inter = ad
                    adIsLoading = false
                    onEvent?.onLoaded()

                    FirebaseManager.sendLog("inter_loaded",null)
                    if (MetaBrainApp.debug) {
                        Log.d(TAG, "Ad inter was loaded.")
                        Toast.makeText(context, "onAdLoaded()", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {

                    inter = null
                    adIsLoading = false
                    onEvent?.onLoadFail()
                    FirebaseManager.sendLog("inter_load_fail",null)
                    if (MetaBrainApp.debug) {
                        Log.d(TAG, adError.message)
                        val error =
                            "domain: ${adError.domain}, code: ${adError.code}, " + "message: ${adError.message}"
                        Toast.makeText(
                            context,
                            "onAdFailedToLoad() with error $error",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
        )
    }

    fun showInter(activity: Activity, onEvent: AdEvent?) {
        if(!FirebaseManager.rc.useAds && DataManager.user.removeAds) {
            onEvent?.onComplete()
            return
        }

        if(!isDuration()){
            if (MetaBrainApp.debug) {
                Log.d(AdsController.Companion.TAG, "Time less than duration config")
            }
            onEvent?.onComplete()
            return
        }

        if (inter != null) {
            Log.d(TAG,"==== show inter")
            FirebaseManager.sendLog("inter_show",null)
            inter?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        if (MetaBrainApp.debug) {
                            Log.d(AdsController.Companion.TAG, "Ad was dismissed.")
                        }
                        timeLastInter = System.currentTimeMillis()
                        inter = null
                        onEvent?.onComplete()
                        FirebaseManager.sendLog("inter_success",null)
                        if(preload) loadInter(activity,currentUnit,loadAction)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        if (MetaBrainApp.debug) {
                            Log.d(AdsController.Companion.TAG, "Ad failed to show.")
                        }
                        inter = null
                        FirebaseManager.sendLog("inter_show_fail",null)
                        if(preload) loadInter(activity,currentUnit,loadAction)
                    }

                    override fun onAdShowedFullScreenContent() {
                        if (MetaBrainApp.debug) {
                            Log.d(AdsController.Companion.TAG, "Ad showed fullscreen content.")
                        }
                    }

                    override fun onAdImpression() {
                        if (MetaBrainApp.debug) {
                            Log.d(AdsController.Companion.TAG, "Ad recorded an impression.")
                        }
                    }

                    override fun onAdClicked() {
                        if (MetaBrainApp.debug) {
                            Log.d(AdsController.Companion.TAG, "Ad was clicked.")
                        }
                    }
                }
            inter?.show(activity)
        } else{
            onEvent?.onComplete()
            if(preload) loadInter(activity,currentUnit,loadAction)
            FirebaseManager.sendLog("inter_not_avail",null)
        }

    }

    fun isDuration() : Boolean{
        if (firstStart)
        {
            firstStart = false;
            return (System.currentTimeMillis() - timeLastInter) > 1000 * FirebaseManager.rc.timeFirstInter;
        }
        else
        {
            return (System.currentTimeMillis() - timeLastInter) > 1000 * FirebaseManager.rc.durationInter;
        }
    }

}