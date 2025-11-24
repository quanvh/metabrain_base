package com.meta.brain.module.firstopen

import android.R
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
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager
import com.meta.brain.module.language.LanguageActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Core splash activity for First Open SDK combining previous base logic into one class.
 */
abstract class FOCoreSplashActivity : AppCompatActivity() {

    companion object {
        @Deprecated("Use constant value directly")
        const val MAX_TIME_SPLASH_AWAIT = 3000L
    }

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutRes())

        supportActionBar?.hide()
        actionBar?.hide()

        updateUI(savedInstanceState)

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

            }
        } catch (e: Exception) {
            e.printStackTrace()
            FirebaseCrashlytics.getInstance().recordException(e)
            startMain()
        }
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
            Log.e("FOCoreSplashActivity", "Error updating locale: ${e.message}")
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

                startActivity(intent)
            } else {
                val activityClass = DataManager.mainActivity
                if (activityClass != null) {
                    val intent = Intent(this, activityClass)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    throw IllegalStateException("MainActivity init first!")
                }
            }
        }
    }
}

