package com.meta.brain.module.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.google.android.gms.ads.MobileAds
import com.meta.brain.R
import com.meta.brain.module.base.MetaBrainApp
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdsController () {

    companion object {
        private lateinit var appOpen: AppOpen
        private lateinit var appOpenResume: AppOpen
        private lateinit var adInter : AdsInter
        private lateinit var adInterOpen : AdsInter
        private lateinit var adInterResume: AdsInter
        private lateinit var adsReward: AdsReward
        private lateinit var adsNative: AdsNative
        private lateinit var adsBanner: AdsBanner

        const val TAG = "[AdsController]"


        var adOpenCount: Int = 0

        fun isOpenReady(): Boolean {
            return adOpenCount >= 1
        }
        fun initAdmob(context: Context) {

//             Log the Mobile Ads SDK version.
            if(MetaBrainApp.debug) {
                Log.d(TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion())
            }

            CoroutineScope(Dispatchers.IO).launch {
                MobileAds.initialize(context) {
                    if(MetaBrainApp.debug){
                        Log.d(TAG, "Init admob success")
                    }
                    //init inter default, call load function if preload
                    val preloadInter = context.resources.getBoolean(R.bool.preload_inter)
                    if(preloadInter) {
                        adInter = AdsInter(true)
                        loadInter(context, null)
                    } else {
                        adInter = AdsInter(false)
                    }

                    adInterOpen = AdsInter()
                    adInterResume = AdsInter()
                    appOpen = AppOpen()
                    appOpenResume = AppOpen()

                    //init reward default, call load function if preload
                    val preloadReward = context.resources.getBoolean(R.bool.preload_reward)
                    if(preloadReward) {
                        adsReward = AdsReward(true)
                        loadReward(context, null)
                    } else {
                        adsReward = AdsReward(false)
                    }


                    adsNative = AdsNative()
                    adsBanner = AdsBanner()

                    if(FirebaseManager.rc.useAds && !DataManager.user.removeAds){
                        val adOpenEvent = object : AdEvent() {
                            override fun onLoaded() {
                                adOpenCount++
                            }
                            override fun onLoadFail() {
                                adOpenCount++
                            }
                        }

                        if(FirebaseManager.rc.useInterOpen) {
                            loadInterOpen(context,adOpenEvent)
                        } else if (FirebaseManager.rc.useOpenSplash){
                            loadOpenAdSplash(context,adOpenEvent)
                        } else {
                            adOpenCount++
                        }
                    }
                    else{
                        adOpenCount++
                    }
                }
            }
        }

        fun loadInter(context: Context,onEvent: AdEvent?){
            var interDefault = context.getString(R.string.inter_default)
            if(!MetaBrainApp.debug && FirebaseManager.adUnit.interDefault.isNotEmpty()) {
                interDefault = FirebaseManager.adUnit.interDefault
            }
            adInter.loadInter(context,interDefault,onEvent)
        }

        fun loadInterResume(context: Context,onEvent: AdEvent?){
            var interResume = context.getString(R.string.inter_resume)
            if(!MetaBrainApp.debug && FirebaseManager.adUnit.interResume.isNotEmpty()) {
                interResume = FirebaseManager.adUnit.interResume
            }
            adInterResume.loadInter(context,interResume,onEvent)
        }

        fun loadInterOpen(context: Context,onEvent: AdEvent?){
            var interOpen = context.getString(R.string.inter_open)
            if(!MetaBrainApp.debug && FirebaseManager.adUnit.interOpen.isNotEmpty()) {
                interOpen = FirebaseManager.adUnit.interOpen
            }
            adInterOpen.loadInter(context,interOpen,onEvent)
        }

        fun showInter(activity: Activity, onEvent: AdEvent?){
            adInter.showInter(activity,onEvent)
        }

        fun showInterResume(activity: Activity, onEvent: AdEvent?){
            adInterResume.showInter(activity,onEvent)
        }

        fun showInterOpen(activity: Activity, onEvent: AdEvent?){
            adInterOpen.showInter(activity,onEvent)
        }

        fun loadOpenAdSplash(context: Context,onEvent: AdEvent?){
            var openSplash = context.getString(R.string.open_splash)
            if(!MetaBrainApp.debug && FirebaseManager.adUnit.openSplash.isNotEmpty()){
               openSplash = FirebaseManager.adUnit.openSplash
            }

            appOpen.loadOpenAd(context, openSplash,onEvent)
        }
        fun loadOpenAdResume(context: Context, onEvent: AdEvent?){
            var openResume = context.getString(R.string.open_resume)
            if(!MetaBrainApp.debug && FirebaseManager.adUnit.openResume.isNotEmpty()){
                openResume = FirebaseManager.adUnit.openResume
            }
            appOpenResume.loadOpenAd(context, openResume,onEvent)
        }
        fun showOpenAd(activity: Activity, onEvent: AdEvent?){
            appOpen.showOpenAd(activity,onEvent)
        }
        fun showOpenAdResume(activity: Activity, onEvent: AdEvent?){
            appOpenResume.showOpenAd(activity,onEvent)
        }

        fun loadReward(context: Context,onEvent: AdEvent?){
            var rewardDefault = context.getString(R.string.reward_default)
            if(!MetaBrainApp.debug && FirebaseManager.adUnit.rewardDefault.isNotEmpty()){
                rewardDefault = FirebaseManager.adUnit.rewardDefault
            }
            adsReward.loadReward(context,rewardDefault,onEvent)
        }

        fun showReward(activity: Activity, onEvent: AdEvent?){
            adsReward.showReward(activity,onEvent)
        }

        /* demo call function
        val binding = NativeDefaultBinding.inflate(layoutInflater)
        val adapter = NativeDefaultBindingAdapter(binding)
        AdsController.loadNative(this@LoadingActivity,
            getString(R.string.native_home),
            findViewById<FrameLayout>(R.id.ad_test),
            adapter)
         */


        fun loadNative(activity: Activity, adUnit:String, container: ViewGroup, adapter: NativeAdViews){
            CoroutineScope(Dispatchers.Main).launch {
                container.removeAllViews()
                container.addView(adapter.root)
                adsNative.loadNative(activity,adUnit,adapter,null)
            }
        }

        //Demo call banner function
        // AdsController.loadBanner(this@LoadingActivity,getString(R.string.banner_default),findViewById<FrameLayout>(R.id.ad_test))
        fun loadBanner(context: Context, adUnit:String, container: ViewGroup){
            adsBanner.loadBanner(context,adUnit,container)
        }
    }
}

public abstract class AdEvent{
    open fun onLoaded(){}
    open fun onLoadFail(){}
    open fun onShow(){}
    open fun onShowFail(){}
    open fun onComplete(){}
    open fun onImpress(){}
    open fun onClick(){}
}