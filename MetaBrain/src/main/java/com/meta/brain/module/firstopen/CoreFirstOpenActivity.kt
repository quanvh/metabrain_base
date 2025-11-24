package com.meta.brain.module.firstopen

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.meta.brain.module.data.DataManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Core base activity for First Open SDK
 */
abstract class CoreFirstOpenActivity : AppCompatActivity() {

    private val _lifecycleEventState = MutableStateFlow<Lifecycle.Event>(Lifecycle.Event.ON_ANY)
    val lifecycleEventState: StateFlow<Lifecycle.Event> = _lifecycleEventState.asStateFlow()

    private val lifecycleEventObserver = LifecycleEventObserver { _, event ->
        _lifecycleEventState.value = event
    }

    abstract fun updateUI(savedInstanceState: Bundle?)

    @LayoutRes
    protected abstract fun getLayoutRes(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycle.addObserver(lifecycleEventObserver)
        super.onCreate(savedInstanceState)
        setContentView(getLayoutRes())

        supportActionBar?.hide()
        actionBar?.hide()

        updateUI(savedInstanceState)
    }


    override fun attachBaseContext(newBase: Context) {
        val languageCode = try {
            DataManager.user.language
        } catch (e: Exception) {
            "en"
        }

        val context = updateLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }

    private fun updateLocale(context: Context, language: String): Context {
        return try {
            val locale = Locale.forLanguageTag(language)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.createConfigurationContext(config)
        } catch (e: Exception) {
            Log.e("CoreFirstOpenActivity", "Error updating locale: ${e.message}")
            context
        }
    }

    /**
     * Set status bar color
     * This is the method that FOCoreSplashActivity calls
     */
    fun setStatusBarColor(@ColorInt color: Int) {
       // TODO
    }
}

