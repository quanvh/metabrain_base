package com.meta.brain.module.base

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.google.android.gms.ads.AdActivity
import com.appsflyer.deeplink.DeepLink
import com.meta.brain.R
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager
import com.meta.brain.module.ads.AdsController
import com.meta.brain.module.loading.AdType
import com.meta.brain.module.loading.LoadingAdFragment


open class MetaBrainApp: Application(), Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    var currentActivity: Activity? = null

    companion object{
        var debug: Boolean = false
        private const val TAG = "[MetaBrainApp]"
    }


    override fun onCreate() {
        super<Application>.onCreate()
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        //Init Appsflyer immediate when app start
        AppsFlyerLib.getInstance().init(getString(R.string.af_key), conversionDataListener, this)
        AppsFlyerLib.getInstance().start(this)
    }

    private var conversionData: MutableMap<String, Any>? = null
    val conversionDataListener  = object : AppsFlyerConversionListener {
        override fun onConversionDataSuccess(conversionDataMap: MutableMap<String, Any>?) {
            val data = conversionDataMap ?: run {
                if(debug) {
                    Log.w(TAG, "AF Conversion data is null")
                }
                return
            }

            // Log toàn bộ key/value
            for ((key, value) in data) {
                if (debug) {
                    Log.d(TAG, "AF Conversion attribute: $key = $value")
                }
            }

            val status = data["af_status"]?.toString() ?: run {
                if(debug) {
                    Log.w(TAG, "AF af_status is missing")
                }
                return
            }
//            val media = data["media_source"]?.toString()
//            val camp  = data["campaign"]?.toString()
            if (status.equals("Non-organic", ignoreCase = true)) {
                DataManager.user.organic = false
                val isFirstLaunch = data["is_first_launch"]?.toString()?.equals("true", ignoreCase = true) == true
                if (isFirstLaunch) {
                    if(debug) {
                        Log.d(TAG, "AF Conversion: First Launch")
                    }
                    DataManager.user.firstInstall = true
                } else {
                    if(debug) {
                        Log.d(TAG, "AF Conversion: Not First Launch")
                    }
                    DataManager.user.firstInstall = false
                }
            } else {
                if(debug) {
                    Log.d(TAG, "Conversion: This is an organic install.")
                }
                DataManager.user.organic = true
            }

            conversionData = data
        }

        override fun onConversionDataFail(error: String?) {
            if(debug) {
                Log.e(TAG, "error onAttributionFailure :  $error")
            }
        }
        override fun onAppOpenAttribution(data: MutableMap<String, String>?) {
            // Must be override to satisfy the AppsFlyerConversionListener interface.
            // Business logic goes here when UDL is not implemented.
            if(debug) {
                data?.map {
                    Log.d(TAG, "onAppOpen_attribute: ${it.key} = ${it.value}")
                }
            }
        }
        override fun onAttributionFailure(error: String?) {
            // Must be override to satisfy the AppsFlyerConversionListener interface.
            // Business logic goes here when UDL is not implemented.
            if(debug) {
                Log.e(TAG, "error onAttributionFailure :  $error")
            }
        }
    }

    /**
     * Danh sách các Activity mặc định không hiển thị quảng cáo khi resume.
     * Danh sách này luôn được áp dụng.
     */
    private fun getDefaultExcludedActivities(): List<Class<out Activity>> {
        return listOf(
            com.meta.brain.module.loading.LoadingActivity::class.java,
            com.meta.brain.module.language.LanguageActivity::class.java,
            com.meta.brain.module.firstopen.FOSplashActivity::class.java,
            AdActivity::class.java
        )
    }

    /**
     * Danh sách các Activity bổ sung không hiển thị quảng cáo khi resume.
     * Có thể override method này để thêm các Activity mới vào danh sách loại trừ.
     * Danh sách mặc định (LoadingActivity, LanguageActivity, AdActivity) sẽ luôn được giữ lại.
     * 
     * Ví dụ:
     * override fun getAdditionalExcludedActivities(): List<Class<out Activity>> {
     *     return listOf(
     *         SplashActivity::class.java,
     *         ProxyBillingActivity::class.java,
     *         ModuleRate.getFeedbackClazz()
     *     )
     * }
     */
    protected open fun getAdditionalExcludedActivities(): List<Class<out Activity>> {
        return emptyList()
    }

    /**
     * Lấy danh sách đầy đủ các Activity không hiển thị quảng cáo khi resume.
     * Bao gồm danh sách mặc định + danh sách bổ sung từ override.
     */
    private fun getAllExcludedActivities(): List<Class<out Activity>> {
        return getDefaultExcludedActivities() + getAdditionalExcludedActivities()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        val activity = currentActivity ?: return
        val activityName = activity.localClassName

        // Validate activity type
        if (activity !is AppCompatActivity) {
            if (debug) Log.d(TAG, "Activity $activityName resumed (not AppCompatActivity)")
            return
        }

        // Check excluded list
        val isExcluded = getAllExcludedActivities().contains(activity.javaClass)
        if (isExcluded) {
            if (debug) Log.d(TAG, "Activity $activityName resumed (excluded)")
            return
        }

        if (debug) Log.d(TAG, "Activity $activityName resumed, show open ads")

        // Show ad
        showResumeAd(activity)
    }

    private fun showResumeAd(activity: AppCompatActivity) {
        // Kiểm tra activity còn hợp lệ không
        if (activity.isFinishing || activity.isDestroyed) {
            if (debug) Log.d(TAG, "Activity is finishing or destroyed, skip showing resume ad")
            return
        }

        // Kiểm tra FragmentManager còn hợp lệ không
        val fragmentManager = activity.supportFragmentManager
        if (fragmentManager.isStateSaved || fragmentManager.isDestroyed) {
            if (debug) Log.d(TAG, "FragmentManager is destroyed or state saved, skip showing resume ad")
            return
        }

        // Kiểm tra xem đã có fragment đang hiển thị chưa
        val existingFragment = fragmentManager.findFragmentByTag(LoadingAdFragment.TAG)
        if (existingFragment != null && existingFragment.isAdded) {
            if (debug) Log.d(TAG, "LoadingAdFragment already showing, skip")
            return
        }

        val adType = when {
            FirebaseManager.rc.useOpenResume -> AdType.APP_OPEN
            FirebaseManager.rc.useInterResume -> AdType.INTERSTITIAL
            else -> return
        }

        try {
            LoadingAdFragment
                .newInstance(adType, true)
                .show(fragmentManager, LoadingAdFragment.TAG)
        } catch (e: IllegalStateException) {
            // FragmentManager đã bị destroy, log và bỏ qua
            if (debug) {
                Log.e(TAG, "Failed to show resume ad: ${e.message}")
            }
        }
    }


    // App đi background
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if(debug) {
            Log.d(TAG, "==== App change to background")
        }

        // Preload ads cho luồng resume (tuỳ chỉnh qua Remote Config)
        if (FirebaseManager.rc.useAds && FirebaseManager.rc.usePreloadResumeAds) {
            try {
                // Preload Open Resume nếu được bật
                if (FirebaseManager.rc.useOpenResume) {
                    AdsController.loadOpenAdResume(applicationContext, null)
                }
                // Preload Inter Resume nếu được bật
                if (FirebaseManager.rc.useInterResume) {
                    AdsController.loadInterResume(applicationContext, null)
                }
            } catch (e: Exception) {
                if (debug) {
                    Log.e(TAG, "Error while preloading resume ads: ${e.message}")
                }
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        if(debug) {
            Log.d(TAG, "Activity Resumed: ${activity.localClassName}")
        }
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

}