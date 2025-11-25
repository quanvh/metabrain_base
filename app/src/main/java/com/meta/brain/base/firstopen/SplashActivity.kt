package com.meta.brain.base.firstopen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
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
import com.meta.brain.module.firstopen.LanguageAdConfig
import com.meta.brain.module.firstopen.NativeConfig
import com.meta.brain.module.language.LanguageActivity
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

    private lateinit var ump: UMP
    private var loadingJob: Job? = null
    private var timeWait: Int = 0
    private var isStartMain = false

    // Store ad config locally for passing to LanguageActivity
    private val templateAdConfig: FOTemplateAdConfig by lazy {
        createTemplateAdConfig()
    }

    override fun getLayoutRes(): Int {
        return R.layout.splash_activity
    }

    override fun updateUI(savedInstanceState: Bundle?) {
        // Initialize DataManager
        DataManager.init(this)

        // Initialize Firebase
        FirebaseManager.initFirebase(this, object : RemoteEvent() {
            override fun onFetched() {
                // Ensure we're on main thread for UI operations
                runOnUiThread {
                    ump = UMP.getInstance(this@SplashActivity)
                    ump.gatherConsent(this@SplashActivity) { consentError ->
                        if (consentError != null) {
                            Log.w(TAG, "${consentError.errorCode}: ${consentError.message}")
                        }

                        if (ump.canRequestAds) {
                            // AdsController.initAdmob must be called on main thread
                            AdsController.initAdmob(this@SplashActivity)
                        }
                    }
                }
            }
        })
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
            // If no ads are shown, nextScreen() will be called automatically by parent class
        }
    }

    override fun nextScreen(activity: ComponentActivity, data: Intent) {
        // This is called by FOSplashActivity after interceptorShowFullScreenAd completes
        // Handle navigation logic here
        if (isStartMain) {
            return
        }
        isStartMain = true

        if (!isDestroyed) {
            if (DataManager.user.firstOpen && FirebaseManager.rc.useLanguageOpen) {
                // Navigate to LanguageActivity with ad config
                val intent = Intent(this, LanguageActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                // Pass ad config via Intent extras
                intent.putExtra(FOTemplateAdConfig.ARG_BUNDLE, templateAdConfig)

                startActivity(intent)
                finish()
            } else {
                checkUpdateAndNavigate()
            }
        }
    }

    private fun createTemplateAdConfig(): FOTemplateAdConfig {
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


    private fun checkUpdateAndNavigate() {
        var currentCode = 0L
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            currentCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting package info", e)
        }

        if (FirebaseManager.appVersion.isForce && FirebaseManager.appVersion.versionCode > currentCode) {
            showUpdateDialog()
        } else {
            startMainActivity()
        }
    }

    private fun startMainActivity() {
        if (!isDestroyed) {
            val activityClass = DataManager.mainActivity
            if (activityClass != null) {
                val intent = Intent(this, activityClass)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                throw IllegalStateException("MainActivity init first!")
            }
        }
    }
}

