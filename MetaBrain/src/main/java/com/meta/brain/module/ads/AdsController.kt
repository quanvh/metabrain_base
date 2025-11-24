package com.meta.brain.module.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.appsflyer.AFAdRevenueData
import com.appsflyer.AppsFlyerLib
import com.appsflyer.MediationNetwork
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.ResponseInfo
import com.meta.brain.R
import com.meta.brain.module.base.MetaBrainApp
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager
import com.meta.brain.module.utils.Utility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdsController() {

    companion object {
        private lateinit var appOpen: AppOpen
        private lateinit var appOpenResume: AppOpen
        private lateinit var adInter: AdsInter
        private lateinit var adInterOpen: AdsInter
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
            val isLoadAds = FirebaseManager.rc.useAds && !DataManager.user.removeAds
                    && !(FirebaseManager.rc.checkBot && Utility.isBot(context))
//             Log the Mobile Ads SDK version.
            if (MetaBrainApp.debug) {
                Log.d(TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion())
            }

            CoroutineScope(Dispatchers.IO).launch {
                MobileAds.initialize(context)
                // Switch to main thread for all ad loading operations
                // InterstitialAd.load() and other ad operations must run on main thread
                withContext(Dispatchers.Main) {
                    if (MetaBrainApp.debug) {
                        Log.d(TAG, "Init admob success")
                    }
                    //init inter default, call load function if preload
                    val preloadInter = context.resources.getBoolean(R.bool.preload_inter)
                    adInter = AdsInter(preloadInter)

                    adInterOpen = AdsInter()
                    adInterResume = AdsInter()
                    appOpen = AppOpen()
                    appOpenResume = AppOpen()

                    //init reward default, call load function if preload
                    val preloadReward = context.resources.getBoolean(R.bool.preload_reward)
                    adsReward = AdsReward(preloadReward)

                    adsNative = AdsNative()
                    adsBanner = AdsBanner()

                    fun preloadAds() {
                        if (preloadInter) {
                            loadInter(context, null)
                        }
                        if (preloadReward) {
                            loadReward(context, null)
                        }
                    }

                    fun initApp() {
                        adOpenCount++
                        preloadAds()
                    }
                    if (isLoadAds) {
                        val adOpenEvent = object : AdEvent() {
                            override fun onLoaded() = initApp()
                            override fun onLoadFail() = initApp()
                        }

                        if (FirebaseManager.rc.useInterOpen) {
                            loadInterOpen(context, adOpenEvent)
                        } else if (FirebaseManager.rc.useOpenSplash) {
                            loadOpenAdSplash(context, adOpenEvent)
                        } else initApp()
                    } else {
                        initApp()
                    }
                }
            }

        }

        fun loadInter(context: Context, onEvent: AdEvent?) {
            if (FirebaseManager.rc.useInterDefault) {
                var interDefault = context.getString(R.string.inter_default)
                if (!MetaBrainApp.debug && FirebaseManager.adUnit.interDefault.isNotEmpty()) {
                    interDefault = FirebaseManager.adUnit.interDefault
                }
                adInter.loadInter(context, interDefault, onEvent)
            } else {
                onEvent?.onLoaded()
            }
        }

        fun loadInterResume(context: Context, onEvent: AdEvent?) {
            if (FirebaseManager.rc.useInterResume) {
                var interResume = context.getString(R.string.inter_resume)
                if (!MetaBrainApp.debug && FirebaseManager.adUnit.interResume.isNotEmpty()) {
                    interResume = FirebaseManager.adUnit.interResume
                }
                adInterResume.loadInter(context, interResume, onEvent)
            } else {
                onEvent?.onLoaded()
            }
        }

        fun loadInterOpen(context: Context, onEvent: AdEvent?) {
            if (FirebaseManager.rc.useInterOpen) {
                var interOpen = context.getString(R.string.inter_open)
                if (!MetaBrainApp.debug && FirebaseManager.adUnit.interOpen.isNotEmpty()) {
                    interOpen = FirebaseManager.adUnit.interOpen
                }
                adInterOpen.loadInter(context, interOpen, onEvent)
            } else {
                onEvent?.onLoaded()
            }
        }

        fun showInter(activity: Activity, onEvent: AdEvent?) {
            if (FirebaseManager.rc.useInterDefault) {
                adInter.showInter(activity, onEvent)
            } else {
                onEvent?.onComplete()
            }
        }

        fun showInterResume(activity: Activity, onEvent: AdEvent?) {
            if (FirebaseManager.rc.useInterResume) {
                adInterResume.showInter(activity, onEvent)
            } else {
                onEvent?.onComplete()
            }
        }

        fun showInterOpen(activity: Activity, onEvent: AdEvent?) {
            if (FirebaseManager.rc.useInterOpen) {
                adInterOpen.showInter(activity, onEvent)
            } else {
                onEvent?.onComplete()
            }
        }

        fun loadOpenAdSplash(context: Context, onEvent: AdEvent?) {
            var openSplash = context.getString(R.string.open_splash)
            if (!MetaBrainApp.debug && FirebaseManager.adUnit.openSplash.isNotEmpty()) {
                openSplash = FirebaseManager.adUnit.openSplash
            }

            appOpen.loadOpenAd(context, openSplash, onEvent)
        }

        fun loadOpenAdResume(context: Context, onEvent: AdEvent?) {
            if (FirebaseManager.rc.useOpenResume) {
                var openResume = context.getString(R.string.open_resume)
                if (!MetaBrainApp.debug && FirebaseManager.adUnit.openResume.isNotEmpty()) {
                    openResume = FirebaseManager.adUnit.openResume
                }
                appOpenResume.loadOpenAd(context, openResume, onEvent)
            } else {
                onEvent?.onLoaded()
            }
        }

        fun showOpenAd(activity: Activity, onEvent: AdEvent?) {
            if (FirebaseManager.rc.useOpenSplash) {
                appOpen.showOpenAd(activity, onEvent)
            } else {
                onEvent?.onComplete()
            }
        }

        fun showOpenAdResume(activity: Activity, onEvent: AdEvent?) {
            if (FirebaseManager.rc.useOpenResume) {
                appOpenResume.showOpenAd(activity, onEvent)
            } else {
                onEvent?.onComplete()
            }
        }

        fun loadReward(context: Context, onEvent: AdEvent?) {
            var rewardDefault = context.getString(R.string.reward_default)
            if (!MetaBrainApp.debug && FirebaseManager.adUnit.rewardDefault.isNotEmpty()) {
                rewardDefault = FirebaseManager.adUnit.rewardDefault
            }
            adsReward.loadReward(context, rewardDefault, onEvent)
        }

        fun showReward(activity: Activity, onEvent: AdEvent?) {
            adsReward.showReward(activity, onEvent)
        }

        /* demo call function
        val binding = NativeDefaultBinding.inflate(layoutInflater)
        val adapter = NativeDefaultBindingAdapter(binding)
        AdsController.loadNative(this@LoadingActivity,
            getString(R.string.native_home),
            findViewById<FrameLayout>(R.id.ad_test),
            adapter)
         */


        fun loadNative(
            activity: Activity,
            adUnit: String,
            container: ViewGroup,
            adapter: NativeAdViews
        ) {
            CoroutineScope(Dispatchers.Main).launch {
                container.removeAllViews()
                container.addView(adapter.root)
                adsNative.loadNative(activity, adUnit, adapter, null)
            }
        }

        //Demo call banner function
        // AdsController.loadBanner(this@LoadingActivity,getString(R.string.banner_default),findViewById<FrameLayout>(R.id.ad_test))
        fun loadBanner(
            context: Context,
            adUnit: String,
            container: ViewGroup,
            bannerSizeType: BannerSizeType? = null
        ) {
            adsBanner.loadBanner(context, adUnit, container, bannerSizeType)
        }

        fun logAdRevenue(adValue: AdValue, responseInfo: ResponseInfo?) {
            val revenue = adValue.valueMicros / 1000000.0
            val loadedInfo = responseInfo?.loadedAdapterResponseInfo
            val data = AFAdRevenueData(
                (loadedInfo?.adSourceName ?: "admob").lowercase(),
                MediationNetwork.GOOGLE_ADMOB,
                adValue.currencyCode,
                revenue
            )
            val extras: MutableMap<String, Any> = hashMapOf(
                "precision" to adValue.precisionType
            )
            AppsFlyerLib.getInstance().logAdRevenue(data, extras)
        }
    }
}

public abstract class AdEvent {
    open fun onLoaded() {}
    open fun onLoadFail() {}
    open fun onShow() {}
    open fun onShowFail() {}
    open fun onComplete() {}
    open fun onImpress() {}
    open fun onClick() {}
}