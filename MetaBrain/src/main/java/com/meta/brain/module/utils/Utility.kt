package com.meta.brain.module.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Environment
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
    }
}

