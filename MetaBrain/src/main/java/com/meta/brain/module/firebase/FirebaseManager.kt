package com.meta.brain.module.firebase

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.meta.brain.R
import com.google.gson.Gson
import com.meta.brain.module.base.MetaBrainApp

public abstract class RemoteEvent{
    open fun onInit(){}
    open fun onFetched(){}
}
class FirebaseManager {

    companion object {
        var rc: RemoteConfig = RemoteConfig()
        var appVersion = AppVersion()
        var adUnit = AdUnit()
        private const val TAG = "[FirebaseManager]"

        private lateinit var firebaseAnalytics: FirebaseAnalytics

        private var status : FirebaseStatus = FirebaseStatus.UnAvailable

        var available = (status == FirebaseStatus.Available)

        fun initFirebase(context: Context, onRemote: RemoteEvent) {
            status = FirebaseStatus.Initialing
            FirebaseApp.initializeApp(context)

            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            FirebaseCrashlytics.getInstance()
                .setCrashlyticsCollectionEnabled(!MetaBrainApp.debug)

            onRemote.onInit()

            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(
                    if (MetaBrainApp.debug) 0 else 3600
                )
                .build()
            remoteConfig.setConfigSettingsAsync(settings)
            remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

            status = FirebaseStatus.Fetching
            remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Log.d(TAG, "Fetch and activate remote config: $updated")
                } else {
                    Log.w(TAG, "Fetch failed: ${task.exception}")
                }

                val adsConfig = remoteConfig.getString("ads_configs")
                if(adsConfig.isNotEmpty()) {
                    try {
                        rc = Gson().fromJson(adsConfig, RemoteConfig::class.java)
                        Log.d(TAG, "adsConfig = $adsConfig")
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi parse JSON (ads_configs): ${e.message}")
                    }
                }
                val remoteVersion = remoteConfig.getString("app_version")
                if(remoteVersion.isNotEmpty()) {
                    try {
                        appVersion = Gson().fromJson(remoteVersion, AppVersion::class.java)
                        Log.d(TAG, "appVersion = $remoteVersion")
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi parse JSON (app_version): ${e.message}")
                    }

                }
                val remoteAdUnit = remoteConfig.getString("ad_unit")
                if(remoteAdUnit.isNotEmpty()) {
                    try {
                        adUnit = Gson().fromJson(remoteAdUnit, AdUnit::class.java)
                        Log.d(TAG, "adUnit = $remoteAdUnit")
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi parse JSON (app_version): ${e.message}")
                    }

                }

                if (MetaBrainApp.debug) {
                    Log.d(TAG, "Fetch Remote Success: ${task.isSuccessful}")
                }
                status = FirebaseStatus.Available
                firebaseAnalytics.logEvent("fetch_remote_success",null)
                onRemote.onFetched()
            }
        }

        fun sendLog(name: String, buildParams: Bundle?) {
            if(MetaBrainApp.debug){
                Log.d(TAG, "send firebase event: $name")
            }
            if (status == FirebaseStatus.Available) {
                firebaseAnalytics.logEvent(name, buildParams)
            }
        }

    }
}

enum class FirebaseStatus {
    UnAvailable,
    Available,
    Initialing,
    Fetching,
}