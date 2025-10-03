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
import com.appsflyer.deeplink.DeepLink
import com.meta.brain.R
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager
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

    private val excludedActivities = listOf(
        "LoadingActivity",
        "LanguageActivity",
        "AdActivity"
    )

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if(currentActivity !=null) {
            val activityName = currentActivity!!.localClassName
            if (!excludedActivities.contains(activityName) && currentActivity is AppCompatActivity) {
                if(debug) {
                    Log.d(TAG, "Activity $activityName resumed, show open ads")
                }

                if (FirebaseManager.rc.useOpenResume) {
                    LoadingAdFragment.newInstance(LoadingAdFragment.Companion.AdType.APP_OPEN, true)
                        .show(
                            (currentActivity as FragmentActivity).supportFragmentManager,
                            LoadingAdFragment.TAG
                        )
                } else if (FirebaseManager.rc.useInterResume) {
                    LoadingAdFragment.newInstance(
                        LoadingAdFragment.Companion.AdType.INTERSTITIAL,
                        true
                    )
                        .show(
                            (currentActivity as FragmentActivity).supportFragmentManager,
                            LoadingAdFragment.TAG
                        )
                }

            } else {
                if(debug) {
                    Log.d(TAG, "Activity $activityName resumed (excluded)")
                }
            }
        }
    }

    // App đi background
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if(debug) {
            Log.d(TAG, "==== App change to background")
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