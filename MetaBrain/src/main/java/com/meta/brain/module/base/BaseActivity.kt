package com.meta.brain.module.base

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.meta.brain.module.firebase.FirebaseManager
import com.meta.brain.module.utils.Utility

abstract class BaseActivity : AppCompatActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        changeStatusBarColor(android.R.color.transparent)
        Utility.hideNavigationDevice(window)

        val params = Bundle().apply {
            putString("activity", javaClass.simpleName)
        }
        if(FirebaseManager.available){
            FirebaseManager.sendLog("start_screen",params)
        }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        val rootView = findViewById<android.view.View>(android.R.id.content)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            view.updatePadding(
                top = statusBarHeight,
                bottom = navBarHeight
            )

            insets
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    fun changeStatusBarColor(color: Int) {
        window.statusBarColor = ContextCompat.getColor(this, color)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
    }
}