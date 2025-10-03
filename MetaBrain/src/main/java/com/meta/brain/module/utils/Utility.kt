package com.meta.brain.module.utils

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Debug
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.meta.brain.module.data.DataManager
import java.io.File
import java.util.Locale

class Utility {
    companion object {

        fun hideNavigationDevice(window: Window?) {
            if (window != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val windowInsetsController =
                        WindowInsetsControllerCompat(window, window.decorView)

                    windowInsetsController.setSystemBarsBehavior(
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    )
                    windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
                    window.setDecorFitsSystemWindows(true)
                } else {
                    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                }
            }
        }
        fun setLocale(context: Context) {
            var language = DataManager.user.language

            if (DataManager.user.language.isEmpty()) {
                language = Locale.getDefault().language
                DataManager.user.language = language
            }
            val config = Configuration()
            val locale = Locale(language)
            Locale.setDefault(locale)
            config.setLocale(locale)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }

        fun checkPermission(activity: Activity, s: String): Boolean {
                return ContextCompat.checkSelfPermission(activity, s) != 0
        }

        fun getPathSave(path: String?): String {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() +
                    File.separator +
                    path
        }

        fun getAdWidth(context: Context): Int{
            val displayMetrics = context.resources.displayMetrics
            val adWidthPixels = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = (context as Activity).windowManager.currentWindowMetrics
                windowMetrics.bounds.width()
            } else {
                @Suppress("DEPRECATION")
                displayMetrics.widthPixels
            }
            return (adWidthPixels / displayMetrics.density).toInt()
        }

        fun isBot (context : Context) : Boolean {
//            Log.e("====", "is emulator: " + isEmulator()+
//                    ", is debugger: " + isDebugger(context) + ", is sensor: "+notSensor(context))
            return (isEmulator() || isDebugger(context) || notSensor(context) )
        }

        private fun isEmulator() : Boolean {
            return Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.BOARD == "QC_Reference_Phone" //bluestacks
                    || Build.MANUFACTURER.contains("Genymotion")
                    || Build.HOST.startsWith("Build") //MSI App Player
                    || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                    || "google_sdk" == Build.PRODUCT
        }

        private fun isDebugger(context: Context): Boolean {
            return Debug.isDebuggerConnected() ||
                    (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        }

        private fun notSensor(context: Context): Boolean {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            return sensorManager.getSensorList(Sensor.TYPE_ALL).isEmpty()
        }
    }
}

