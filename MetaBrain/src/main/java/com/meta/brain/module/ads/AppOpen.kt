package com.meta.brain.module.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.meta.brain.module.base.MetaBrainApp
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager

class AppOpen {
    companion object {
        private const val TAG: String = "[AppOpen]"
    }
    private var appOpenAd: AppOpenAd? = null
    private var preload : Boolean = false
    private var isLoadingAd = false
    var isShowingAd = false

    private var currentUnit: String = ""

    fun loadOpenAd(context: Context,adUnit:String, onEvent: AdEvent?) {
        if(FirebaseManager.rc.useAds && !DataManager.user.removeAds) {
            if (MetaBrainApp.debug) {
                Log.d(TAG, "OpenAd call, id: $adUnit")
            }
            FirebaseManager.sendLog("openAd_load",null)
            currentUnit = adUnit
            if (isLoadingAd || isAdAvailable()) {
                return
            }

            isLoadingAd = true
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context,
                adUnit,
                request,
                object : AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        FirebaseManager.sendLog("openAd_loaded",null)
                        appOpenAd = ad
                        isLoadingAd = false
                        onEvent?.onLoaded()

                        if (MetaBrainApp.debug) {
                            Log.d(TAG, "OpenAd was loaded")
                            Toast.makeText(context, "onAdLoaded", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        FirebaseManager.sendLog("openAd_load_fail",null)
                        isLoadingAd = false
                        onEvent?.onLoadFail()

                        if (MetaBrainApp.debug) {
                            Log.d(TAG, "OpenAd load failed: " + loadAdError.message)
                            Toast.makeText(context, "onAdFailedToLoad", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
            )
        } else {
            onEvent?.onLoaded()
        }
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null
    }

    fun showOpenAd(activity: Activity, onEvent: AdEvent?) {
        if(FirebaseManager.rc.useAds && !DataManager.user.removeAds) {
            if (isShowingAd) {
                if(MetaBrainApp.debug) {
                    Log.d(TAG, "OpenAd is already showing.")
                }
                return
            }

            if (!isAdAvailable()) {
                if (MetaBrainApp.debug) {
                    Log.d(TAG, "OpenAd is not ready yet.")
                }
                FirebaseManager.sendLog("openAd_not_avail",null)
                onEvent?.onComplete()
                if (preload) loadOpenAd(activity, currentUnit, null)
                return
            }

            FirebaseManager.sendLog("openAd_show",null)

            if (MetaBrainApp.debug) {
                Log.d(TAG, "OpenAd show")
            }

            appOpenAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        appOpenAd = null
                        isShowingAd = false
                        onEvent?.onComplete()
                        if (preload) loadOpenAd(activity, currentUnit, null)

                        if (MetaBrainApp.debug) {
                            Log.d(TAG, "OpenAd show success")

                            Toast.makeText(
                                activity,
                                "onAdDismissedFullScreenContent",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        FirebaseManager.sendLog("openAd_success",null)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        appOpenAd = null
                        isShowingAd = false
                        onEvent?.onComplete()
                        if (preload) loadOpenAd(activity, currentUnit, null)

                        if (MetaBrainApp.debug) {
                            Log.d(TAG, "OpenAd show failed: " + adError.message)
                            Toast.makeText(
                                activity,
                                "onAdFailedToShowFullScreenContent",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        FirebaseManager.sendLog("openAd_show_fail",null)
                    }

                    override fun onAdShowedFullScreenContent() {
                        if (MetaBrainApp.debug) {
                            Log.d(TAG, "OpenAd show")
                            Toast.makeText(activity, "onAdShowedFullScreenContent", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            isShowingAd = true
            appOpenAd?.show(activity)
        } else {
            onEvent?.onComplete()
        }
    }

}