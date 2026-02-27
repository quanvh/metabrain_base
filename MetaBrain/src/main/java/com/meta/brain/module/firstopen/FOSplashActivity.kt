package com.meta.brain.module.firstopen

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.meta.brain.R
import com.meta.brain.module.ads.AdEvent
import com.meta.brain.module.ads.AdsController
import com.meta.brain.module.ads.AdsNative
import com.meta.brain.module.ads.GenericNativeAdViews
import com.meta.brain.module.ads.UMP
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager
import com.meta.brain.module.firebase.RemoteEvent
import com.meta.brain.module.language.LanguageActivity
import com.meta.brain.module.language.LanguageModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * Abstract splash activity for First Open SDK Template 1
 */
abstract class FOSplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "FOSplashActivity"

        @Deprecated("Use constant value directly")
        const val MAX_TIME_SPLASH_AWAIT = 3000L
    }

    private lateinit var ump: UMP

    abstract fun updateUI(savedInstanceState: Bundle?)

    @LayoutRes
    protected abstract fun getLayoutRes(): Int

    abstract fun handleRemoteConfig(remoteConfig: FirebaseRemoteConfig)

    abstract fun getBannerAdView(): FrameLayout?

    @CallSuper
    open fun afterFetchRemote() {
        // Override in subclasses
    }

    private val adsNative = AdsNative()


    // --- Native Fullscreen Configuration ---
    open fun getNativeFullscreenId(): String? = null
    open fun getNativeFullscreenPreloadKey(): String? = "native_fullscreen"
    open fun getNativeFullscreenLayout(): Int? = null

    /**
     * Show Native Fullscreen Ad
     * @param waitClose If true, stays suspended until the ad is closed. If false, returns as soon as ad is loaded/shown.
     */
    // ===============================
    // Native Flow
    // ===============================

    private suspend fun showNativeBeforeInter() {
        val adUnitId = getNativeFullscreenId() ?: return
        val layoutRes = getNativeFullscreenLayout() ?: return
        val preloadKey = getNativeFullscreenPreloadKey() ?: return

        if (!ensureNativePreloaded(adUnitId, preloadKey)) return

        if (renderNativeFullscreen(adUnitId, layoutRes, preloadKey)) {
            delay(600)
            removeNativeFullscreen(preloadKey)
        }
    }

    private suspend fun showNativeAfterInter() {
        val adUnitId = getNativeFullscreenId() ?: return
        val layoutRes = getNativeFullscreenLayout() ?: return
        val preloadKey = (getNativeFullscreenPreloadKey() ?: return) + "_after"

        adsNative.preloadNativeAd(this, adUnitId, preloadKey)

        if (!ensureNativePreloaded(adUnitId, preloadKey)) return

        if (renderNativeFullscreen(adUnitId, layoutRes, preloadKey)) {
            delay(600)
            removeNativeFullscreen(preloadKey)
        }
    }

    private suspend fun ensureNativePreloaded(
        adUnitId: String,
        preloadKey: String
    ): Boolean = suspendCancellableCoroutine { cont ->

        val cachedAd = adsNative.getPreloadedAd(preloadKey)
        if (cachedAd != null) {
            cont.resume(true) {}
            return@suspendCancellableCoroutine
        }

        adsNative.preloadNativeAd(this, adUnitId, preloadKey, object : AdEvent() {
            override fun onLoaded() {
                if (cont.isActive) cont.resume(true) {}
            }

            override fun onLoadFail() {
                if (cont.isActive) cont.resume(false) {}
            }
        })
    }

    private suspend fun renderNativeFullscreen(
        adUnitId: String,
        layoutRes: Int,
        preloadKey: String
    ): Boolean = suspendCancellableCoroutine { cont ->

        runOnUiThread {
            try {
                val containerId =
                    resources.getIdentifier("fl_ad_native", "id", packageName)

                val container =
                    if (containerId != 0) findViewById<FrameLayout>(containerId) else null

                if (container == null) {
                    cont.resume(false) {}
                    return@runOnUiThread
                }

                val adsView = layoutInflater.inflate(
                    layoutRes,
                    null
                ) as com.google.android.gms.ads.nativead.NativeAdView

                container.visibility = View.VISIBLE
                container.removeAllViews()

                container.addView(
                    adsView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )

                val genericViews = GenericNativeAdViews(adsView)

                adsNative.loadNative(
                    this,
                    adUnitId,
                    genericViews,
                    object : AdEvent() {

                        override fun onImpress() {
                            if (cont.isActive) cont.resume(true) {}
                        }

                        override fun onLoadFail() {
                            removeNativeFullscreen(preloadKey)
                            if (cont.isActive) cont.resume(false) {}
                        }
                    },
                    container,
                    preloadKey
                )

            } catch (e: Exception) {
                cont.resume(false) {}
            }
        }

        cont.invokeOnCancellation {
            removeNativeFullscreen(preloadKey)
        }
    }

    private fun removeNativeFullscreen(preloadKey: String) {
        runOnUiThread {
            try {
                val containerId =
                    resources.getIdentifier("fl_ad_native", "id", packageName)

                val container =
                    if (containerId != 0) findViewById<FrameLayout>(containerId) else null

                container?.removeAllViews()
                container?.visibility = View.GONE

                adsNative.destroyAd(preloadKey)

            } catch (_: Exception) {
            }
        }
    }


    /**
     * Interceptor hook for showing full screen ads before navigation
     * Called after splash screen delay and before navigating to main screen
     * Override in subclasses to show full screen ads or perform other async operations
     */

    // ===============================
    // Interceptor Flow
    // ===============================

    open suspend fun interceptorShowFullScreenAd() = coroutineScope {

        // 1️⃣ Native A
        showNativeBeforeInter()

        val adReady = awaitOpenAdOrTimeout(30_000)

        if (FirebaseManager.rc.useAds && adReady) {

            val nativeAfterKey =
                (getNativeFullscreenPreloadKey() ?: return@coroutineScope) + "_after"

            val nativeAfterId = getNativeFullscreenId()

            // 2️⃣ Chạy song song:
            // - Show Inter
            // - Preload Native B

            val preloadJob = if (nativeAfterId != null) {
                launch {
                    adsNative.preloadNativeAd(
                        this@FOSplashActivity,
                        nativeAfterId,
                        nativeAfterKey
                    )
                }
            } else null

            // Show Inter và chờ close
            showOpenAdsAndWait()
        }

        // 3️⃣ Native B
        showNativeAfterInter()

        startMain()
    }

    private suspend fun awaitOpenAdOrTimeout(timeoutMs: Long): Boolean =
        withTimeoutOrNull(timeoutMs) {
            while (!AdsController.isOpenReady()) {
                delay(200)
            }
            true
        } ?: false

    /**
     * Get template ad configuration for LanguageActivity
     * Override in subclasses to provide ad configuration
     * @return FOTemplateAdConfig or null if no ad config needed
     */
    open fun getTemplateAdConfig(): FOTemplateAdConfig? {
        // Override in subclasses to provide ad config
        return null
    }

    /**
     * Initialize template UI configuration for LanguageActivity
     * Override in subclasses to provide custom UI configurations
     * @return FOTemplateUiConfig or null if using default UI
     */
    open fun initTemplateUiConfig(): FOTemplateUiConfig? {
        // Override in subclasses to provide custom UI config
        return null
    }

    /**
     * Called when navigating to MainActivity (not first open or language screen not needed)
     * Override in subclasses to add custom logic like update check
     */
    open fun onNavigateToMain() {
        // Default: navigate directly to MainActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutRes())

        supportActionBar?.hide()
        actionBar?.hide()

        updateUI(savedInstanceState)
        initData()

        setStatusBarColor(resources.getColor(R.color.white))

        try {
            lifecycleScope.launch {
                val startTime = System.currentTimeMillis()

                // Call afterFetchRemote hook
                afterFetchRemote()

                // Calculate remaining time to wait (max 3000ms)
                val timeAwaitSplash =
                    (MAX_TIME_SPLASH_AWAIT - (System.currentTimeMillis() - startTime))
                        .coerceIn(0L, MAX_TIME_SPLASH_AWAIT)

                // Wait for remaining time
                Log.d("KhanhNV", "timeAwaitSplash: $timeAwaitSplash")
                delay(timeAwaitSplash)

                // Call interceptor hook
                interceptorShowFullScreenAd()

                // Navigate to next screen
//                startMain()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            FirebaseCrashlytics.getInstance().recordException(e)
            startMain()
        }
    }

    private fun initData() {
        DataManager.init(this)

        // Initialize Firebase
        FirebaseManager.initFirebase(this, object : RemoteEvent() {
            override fun onFetched() {
                // Ensure we're on main thread for UI operations
                runOnUiThread {
                    ump = UMP.getInstance(this@FOSplashActivity)
                    ump.gatherConsent(this@FOSplashActivity) { consentError ->
                        if (consentError != null) {
                            Log.w(TAG, "${consentError.errorCode}: ${consentError.message}")
                        }

                        if (ump.canRequestAds) {
                            // AdsController.initAdmob must be called on main thread
                            AdsController.initAdmob(this@FOSplashActivity)
                        }
                    }
                }
            }
        })
    }

    override fun attachBaseContext(newBase: Context) {
        val languageCode = try {
            DataManager.user.language
        } catch (e: Exception) {
            "en"
        }

        val context = updateLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }

    private fun updateLocale(context: Context, language: String): Context {
        return try {
            val locale = Locale.forLanguageTag(language)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.createConfigurationContext(config)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating locale: ${e.message}")
            context
        }
    }

    /**
     * Set status bar color previously provided by CoreFirstOpenActivity.
     */
    fun setStatusBarColor(@ColorInt color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = color
        }
    }

    private fun startMain() {
        Log.d(TAG, "startMain: ")
        if (!isDestroyed) {
            if (DataManager.user.firstOpen && FirebaseManager.rc.useLanguageOpen) {
                val intent = Intent(this, LanguageActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                // Pass ad config if available
                val adConfig = getTemplateAdConfig()
                if (adConfig != null) {
                    intent.putExtra(FOTemplateAdConfig.ARG_BUNDLE, adConfig)
                }

                // Pass UI config if available
                val uiConfig = initTemplateUiConfig()
                if (uiConfig != null) {
                    intent.putExtra(FOTemplateUiConfig.ARG_BUNDLE, uiConfig)
                }

                startActivity(intent)
                finish()
            } else {
                onNavigateToMain()
            }
        }
    }

    private suspend fun showOpenAdsAndWait() {
        suspendCancellableCoroutine { cont ->
            when {
                FirebaseManager.rc.useInterOpen -> {
                    Log.d(TAG, "showInterOpen")
                    AdsController.showInterOpen(
                        this@FOSplashActivity,
                        object : AdEvent() {
                            override fun onComplete() {
                                if (cont.isActive) cont.resume(Unit) {}
                            }
                        }
                    )
                }

                FirebaseManager.rc.useOpenSplash -> {
                    Log.d(TAG, "showOpenAd")
                    AdsController.showOpenAd(
                        this@FOSplashActivity,
                        object : AdEvent() {
                            override fun onComplete() {
                                if (cont.isActive) cont.resume(Unit) {}
                            }
                        }
                    )
                }

                else -> {
                    cont.resume(Unit) {}
                }
            }

            cont.invokeOnCancellation {
                Log.d(TAG, "showOpenAdsAndWait cancelled")
            }
        }
    }
}

