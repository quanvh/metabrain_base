package com.meta.brain.base

import android.app.Activity
import androidx.lifecycle.LifecycleOwner
import com.meta.brain.base.firstopen.SplashActivity
import com.meta.brain.module.base.MetaBrainApp
import com.meta.brain.module.data.DataManager

class App : MetaBrainApp() {
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        debug = true
        DataManager.setStartActivity(IntroActivity::class.java)
    }

    override fun getAdditionalExcludedActivities(): List<Class<out Activity>> {
        return listOf(
            SplashActivity::class.java
        )
    }
}