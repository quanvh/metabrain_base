package com.meta.brain.module.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.*
import com.meta.brain.module.base.MetaBrainApp
import com.meta.brain.module.firebase.FirebaseManager

class AdsReward (var preload: Boolean = false){
    companion object {
        private const val TAG = "[AdsReward]"
    }

    private var isLoading = false

    private var currentUnit = ""
    private var loadAction : AdEvent? = null
    private var rewardedAd: RewardedAd? = null

    fun loadReward(context: Context, adUnit:String, onEvent: AdEvent?) {
        currentUnit = adUnit
        loadAction = onEvent
        if (rewardedAd == null) {
            FirebaseManager.sendLog("reward_call",null)
            isLoading = true

            RewardedAd.load(
                context,
                adUnit,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        FirebaseManager.sendLog("reward_loaded",null)
                        rewardedAd = ad
                        isLoading = false
                        onEvent?.onLoaded()
                        if(MetaBrainApp.debug){
                            Log.d(TAG, "Reward Ad was loaded.")
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        FirebaseManager.sendLog("reward_load_fail",null)
                        rewardedAd = null
                        isLoading = false
                        onEvent?.onLoadFail()
                        if(MetaBrainApp.debug){
                            Log.d(TAG, "Reward load error:" + adError.message)
                        }
                    }
                },
            )
        }
    }

    fun showReward(activity: Activity, onEvent: AdEvent?) {
        if (rewardedAd != null) {
            FirebaseManager.sendLog("reward_show",null)
            rewardedAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        rewardedAd = null
                        onEvent?.onComplete()
                        if(preload) loadReward(activity,currentUnit,null)
                        if(MetaBrainApp.debug){
                            Log.d(TAG, "Reward Ad was dismissed.")
                        }
                        FirebaseManager.sendLog("reward_success",null)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        rewardedAd = null
                        onEvent?.onShowFail()
                        if(preload) loadReward(activity,currentUnit,null)
                        if(MetaBrainApp.debug){
                            Log.d(TAG, "Reward Ad failed to show.")
                        }
                        FirebaseManager.sendLog("reward_show_fail",null)
                    }

                    override fun onAdShowedFullScreenContent() {
                        onEvent?.onShow()
                        if(MetaBrainApp.debug) {
                            Log.d(TAG, "Ad showed fullscreen content.")
                        }
                    }

                    override fun onAdImpression() {
                        onEvent?.onImpress()
                        if(MetaBrainApp.debug) {
                            Log.d(TAG, "Ad recorded an impression.")
                        }
                    }

                    override fun onAdClicked() {
                        onEvent?.onClick()
                        if(MetaBrainApp.debug) {
                            Log.d(TAG, "Ad was clicked.")
                        }
                    }
                }

            rewardedAd?.show(
                activity,
                OnUserEarnedRewardListener { rewardItem ->
                    if(MetaBrainApp.debug) {
                        Log.d(TAG, "User earned the reward.")
                    }
                },
            )
        } else {
            if(preload) loadReward(activity,currentUnit,loadAction)
            FirebaseManager.sendLog("reward_not_avail",null)
        }
    }
}