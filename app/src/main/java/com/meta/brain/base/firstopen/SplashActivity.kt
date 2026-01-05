package com.meta.brain.base.firstopen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.meta.brain.base.R
import com.meta.brain.module.ads.AdEvent
import com.meta.brain.module.ads.AdsController
import com.meta.brain.module.ads.UMP
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager
import com.meta.brain.module.firebase.RemoteEvent
import com.meta.brain.module.firstopen.FOSplashActivity
import com.meta.brain.module.firstopen.FOTemplateAdConfig
import com.meta.brain.module.firstopen.FOTemplateUiConfig
import com.meta.brain.module.firstopen.LanguageAdConfig
import com.meta.brain.module.firstopen.LanguageUiConfig
import com.meta.brain.module.firstopen.NativeConfig
import com.meta.brain.module.language.LanguageActivity
import com.meta.brain.module.language.LanguageModel
import com.meta.brain.module.utils.Utility
import com.meta.brain.module.utils.showUpdateDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * First Open Splash Activity implementation for app module
 * Extends FOSplashActivity and uses LoadingActivity layout
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : FOSplashActivity() {

    companion object {
        private const val TAG = "[AppFOSplashActivity]"
        private const val TOTAL_TIME_WAIT = 30
    }

    private var loadingJob: Job? = null
    private var timeWait: Int = 0

    override fun getLayoutRes(): Int {
        return R.layout.splash_activity
    }

    override fun updateUI(savedInstanceState: Bundle?) {
        // Initialize DataManager

    }

    override fun handleRemoteConfig(remoteConfig: FirebaseRemoteConfig) {
        // Handle remote config if needed
        // Override in subclasses for custom remote config handling
    }

    override fun getBannerAdView(): FrameLayout? {
        // Return banner ad view if needed, otherwise null
        return null
    }

    override fun afterFetchRemote() {
        super.afterFetchRemote()
        Utility.setLocale(this)
        checkAds()
    }

    override suspend fun interceptorShowFullScreenAd() {
        // Wait for ads to be ready (already started in afterFetchRemote)
        while (loadingJob != null) {
            delay(100)
        }

        // Show ads if configured
        if (FirebaseManager.rc.useAds) {
            if (FirebaseManager.rc.useInterOpen) {
                val adCompleted = CompletableDeferred<Unit>()
                AdsController.showInterOpen(this, object : AdEvent() {
                    override fun onComplete() {
                        adCompleted.complete(Unit)
                    }
                })
                adCompleted.await()
            } else if (FirebaseManager.rc.useOpenSplash) {
                val adCompleted = CompletableDeferred<Unit>()
                AdsController.showOpenAd(this, object : AdEvent() {
                    override fun onComplete() {
                        adCompleted.complete(Unit)
                    }
                })
                adCompleted.await()
            }
            // If no ads are shown, startMain() will be called automatically by parent class
        }
    }

    override fun getTemplateAdConfig(): FOTemplateAdConfig {
        // Create native ad config for language screen
        return FOTemplateAdConfig(
            languageAdConfig = LanguageAdConfig(
                nativeAdConfig = NativeConfig(
                    adUnitId = getLanguageAdUnitId(),
                    layoutId = getLanguageNativeAdLayoutId()
                )
            )
        )
    }

    /**
     * Get language ad unit ID
     * Override in subclasses to provide actual ad unit ID
     */
    private fun getLanguageAdUnitId(): String {
        return getString(com.meta.brain.R.string.native_home)
    }

    /**
     * Get language native ad layout ID
     * Override in subclasses to provide actual layout ID
     */
    private fun getLanguageNativeAdLayoutId(): Int {
        return R.layout.native_default_no_id_price
    }

    override fun initTemplateUiConfig(): FOTemplateUiConfig {
        // Override this method to provide custom UI configuration
        val listLanguage =  listOf(
            LanguageModel(0, "English (US)","en", true),
            LanguageModel(1, "Vietnamese", "vi", false),
            LanguageModel(2, "French", "fr", false),
            LanguageModel(3, "Spanish", "es", false),
            LanguageModel(4, "India", "hi", false),
            LanguageModel(5, "Portuguese (Brazil)", "pt-BR", false),
            LanguageModel(6, "Indonesia", "in", false),
            LanguageModel(7, "Russian", "ru", false),
            LanguageModel(8, "Turkey", "tr-TR", false),
            LanguageModel(9, "Malaysian", "ms", false),
        )
        val layoutId = R.layout.language_activity_custom
        val itemLayoutId = R.layout.language_item
        val languageUiConfig = LanguageUiConfig(
            layoutId = layoutId,
            itemLayoutId = itemLayoutId,
            listLanguage = listLanguage
        )
        return FOTemplateUiConfig(languageUiConfig = languageUiConfig)
    }

    private fun checkAds() {
        loadingJob = lifecycleScope.launch {
            while (isActive) {
                if (AdsController.isOpenReady() || timeWait >= TOTAL_TIME_WAIT) {
                    loadingJob?.cancel()
                    loadingJob = null
                    break
                } else {
                    timeWait++
                }
                delay(1000)
            }
        }
    }
}
