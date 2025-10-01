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
import com.meta.brain.module.firebase.FirebaseManager
import com.meta.brain.module.loading.LoadingAdFragment

class MetaBrainApp: Application(), Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private var currentActivity: Activity? = null

    companion object{
        var debug: Boolean = false
    }


    override fun onCreate() {
        super<Application>.onCreate()
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private val excludedActivities = listOf(
        "LoadingActivity",
        "AdActivity"
    )

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if(currentActivity !=null) {
            val activityName = currentActivity!!.localClassName
            if (!excludedActivities.contains(activityName) && currentActivity is AppCompatActivity) {
                if(debug) {
                    Log.d("[App]", "==== $activityName resumed, show open ads")
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
                    Log.d("[App]", "==== $activityName resumed (excluded)")
                }
            }
        }
    }

    // App đi background
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if(debug) {
            Log.d("[App]", "==== App change to background")
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        if(debug) {
            Log.d("App", "==== Resumed: ${activity.localClassName}")
        }
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

}