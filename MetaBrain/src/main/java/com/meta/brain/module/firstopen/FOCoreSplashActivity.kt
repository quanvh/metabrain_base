package com.meta.brain.module.firstopen

import android.R
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.annotation.CallSuper
import androidx.lifecycle.lifecycleScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager
import com.meta.brain.module.language.LanguageActivity
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.time.measureTimedValue

/**
 * Core splash activity for First Open SDK
 */
abstract class FOCoreSplashActivity : CoreFirstOpenActivity() {

    companion object {
        @Deprecated("Use constant value directly")
        const val MAX_TIME_SPLASH_AWAIT = 3000L
    }


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

