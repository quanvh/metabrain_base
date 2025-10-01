package com.meta.brain.base

import androidx.lifecycle.LifecycleOwner
import com.meta.brain.module.base.MetaBrainApp
import com.meta.brain.module.data.DataManager

class App : MetaBrainApp() {
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        debug = true
        DataManager.setStartActivity(IntroActivity::class.java)
    }
}