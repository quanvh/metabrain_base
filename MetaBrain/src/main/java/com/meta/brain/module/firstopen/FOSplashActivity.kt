package com.meta.brain.module.firstopen

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.meta.brain.R
import com.meta.brain.module.ads.AdsController
import com.meta.brain.module.ads.UMP
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager
import com.meta.brain.module.firebase.RemoteEvent
import com.meta.brain.module.language.LanguageActivity
import com.meta.brain.module.language.LanguageModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    open suspend fun interceptorShowFullScreenAd() {
        // Override in subclasses
    }

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
                delay(timeAwaitSplash)

                // Call interceptor hook
                interceptorShowFullScreenAd()

                // Navigate to next screen
                startMain()
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
}

