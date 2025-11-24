package com.meta.brain.module.firstopen

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Abstract splash activity for First Open SDK Template 1
 */
abstract class FOSplashActivity : FOCoreSplashActivity() {

    companion object {
        private const val TAG = "FOSplashActivity"
    }

    // Lazy properties
    private val templateUiConfig: FOTemplateUiConfig by lazy {
        initTemplateUiConfig()
    }

    private val templateAdConfig: FOTemplateAdConfig by lazy {
        initTemplateAdConfig()
    }


    abstract fun nextScreen(activity: ComponentActivity, data: Intent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nextScreen(this, intent)
    }


    abstract fun initTemplateUiConfig(): FOTemplateUiConfig

    abstract fun initTemplateAdConfig(): FOTemplateAdConfig




}

