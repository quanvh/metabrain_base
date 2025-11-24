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
import com.meta.brain.module.firstopen.FOTemplateUiConfig
import com.meta.brain.module.firstopen.LanguageAdConfig
import com.meta.brain.module.firstopen.LanguageUiConfig
import com.meta.brain.module.firstopen.NativeConfig
import com.meta.brain.module.language.LanguageActivity
import com.meta.brain.module.language.LanguageModel
import com.meta.brain.module.utils.Utility
import com.meta.brain.module.utils.showUpdateDialog
import kotlinx.coroutines.Job
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
        private const val MAX_TIME_SPLASH_AWAIT = 3000L
        private const val TOTAL_TIME_WAIT = 30
    }

    private lateinit var ump: UMP
    private var loadingJob: Job? = null
    private var timeWait: Int = 0
    private var isStartMain = false

    // Store configs locally since they're private in parent class
    private val templateUiConfig: FOTemplateUiConfig by lazy {
        initTemplateUiConfig()
    }

    private val templateAdConfig: FOTemplateAdConfig by lazy {
        initTemplateAdConfig()
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
                AdsController.showInterOpen(this, object : AdEvent() {
                    override fun onComplete() {
                        navigateToNext()
                    }
                })
            } else if (FirebaseManager.rc.useOpenSplash) {
                AdsController.showOpenAd(this, object : AdEvent() {
                    override fun onComplete() {
                        navigateToNext()
                    }
                })
            } else {
                navigateToNext()
            }
        } else {
            navigateToNext()
        }
    }

    override fun initTemplateUiConfig(): FOTemplateUiConfig {
        // Create language list from LanguageActivity data
        val countryName = listOf(
            "English", "Indonesia", "Portuguese", "Spanish", "India",
            "Turkey", "France", "Vietnamese", "Russian"
        )
        val languageCode = listOf(
            "en", "in", "pt", "es", "hi", "tr", "fr", "vi", "ru"
        )

        val languageList = mutableListOf<LanguageModel>()
        val userPreferred = getPreferredLanguageCode()

        for (i in languageCode.indices) {
            val languageModel = LanguageModel(i, countryName[i], languageCode[i], false)
            languageList.add(languageModel)
        }

        return FOTemplateUiConfig(
            languageUiConfig = LanguageUiConfig(
                layoutId = R.layout.activity_language,
                itemLayoutId = R.layout.item_language,
                listLanguage = languageList,
            )
        )
    }

    override fun initTemplateAdConfig(): FOTemplateAdConfig {
        // Create native ad config for language screen
        // Override in subclasses to provide actual ad unit IDs
        return FOTemplateAdConfig(
            languageAdConfig = LanguageAdConfig(
                nativeAdConfig = NativeConfig(
                    adUnitId = getLanguageAdUnitId(),
                    layoutId = getLanguageNativeAdLayoutId()
                )
            )
        )
    }

    override fun nextScreen(activity: ComponentActivity, data: Intent) {
        // Handle navigation after language selection
        // This is called by FirstOpenSDK after language is selected
        // Similar to LoadingActivity.startMain() but after language is already selected
        // So we go directly to MainActivity (no need to check firstOpen again)
        if (!isDestroyed) {
            // Check if mainActivity is set before navigating
            val activityClass = DataManager.mainActivity
            if (activityClass != null) {
                checkUpdateAndNavigate()
            } else {
                Log.e(
                    TAG,
                    "MainActivity not initialized. Please call DataManager.setStartActivity() first."
                )
                // Don't throw exception, just log error
                // The app will continue to run and mainActivity should be set later
            }
        }
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

    private fun navigateToNext() {
        if (isStartMain) {
            return
        }
        isStartMain = true

        if (!isDestroyed) {
            if (DataManager.user.firstOpen && FirebaseManager.rc.useLanguageOpen) {
                // Navigate to LanguageActivity with configs
                val intent = Intent(this, LanguageActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                // Pass configs via Intent extras
                intent.putExtra(FOTemplateUiConfig.ARG_BUNDLE, templateUiConfig)
                intent.putExtra(FOTemplateAdConfig.ARG_BUNDLE, templateAdConfig)

                startActivity(intent)
                finish()
            } else {
                checkUpdateAndNavigate()
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

    private fun getPreferredLanguageCode(): String {
        val userPreferred = try {
            DataManager.user.language
        } catch (e: Exception) {
            ""
        }

        if (userPreferred.isEmpty()) {
            return java.util.Locale.getDefault().language
        }

        return userPreferred
    }
}

