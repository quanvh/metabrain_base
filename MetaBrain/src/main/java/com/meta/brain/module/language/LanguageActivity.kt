package com.meta.brain.module.language

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.nativead.NativeAdView
import com.meta.brain.R
import com.meta.brain.databinding.LanguageActivityBinding
import com.meta.brain.databinding.NativeDefaultBinding
import com.meta.brain.module.ads.AdsController
import com.meta.brain.module.ads.GenericNativeAdViews
import com.meta.brain.module.ads.NativeAdViews
import com.meta.brain.module.ads.NativeDefaultBindingAdapter
import com.meta.brain.module.base.DataBindActivity
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firstopen.*
import com.meta.brain.module.utils.Utility
import com.meta.brain.module.utils.invisible
import java.util.Locale

class LanguageActivity :
    DataBindActivity<LanguageActivityBinding>(R.layout.language_activity),
    LanguageAdapter.LanguageAdapterCallBack {

    companion object {
        private const val TAG = "[LanguageActivity]"
        const val RESULT_LANGUAGE_SELECTED = Activity.RESULT_OK
        const val EXTRA_SELECTED_LANGUAGE_CODE = "extra_selected_language_code"
        const val EXTRA_SELECTED_LANGUAGE_NAME = "extra_selected_language_name"
        const val EXTRA_SKIP_NAVIGATE_MAIN = "extra_skip_navigate_main"

        val countryName = mutableListOf(
            "English",
            "Indonesia",
            "Portuguese",
            "Spanish",
            "India",
            "Turkey",
            "France",
            "Vietnamese",
            "Russian"
        )

        val languageCode = mutableListOf(
            "en",
            "in",
            "pt",
            "es",
            "hi",
            "tr",
            "fr",
            "vi",
            "ru"
        )
    }

    private var languageModel: LanguageModel? = null
    private var languageAdConfig: LanguageAdConfig? = null
    private var skipNavigateMain: Boolean = false

    override fun initView() {
        readConfigs()

        binding.imgBack.invisible()
        binding.imgBack.setOnClickListener { finish() }

        binding.imgDone.setOnClickListener {
            onDoneClick()
        }
        initLanguageData()
        loadNativeAdIfNeeded()
    }

    private fun readConfigs() {
        languageAdConfig =
            intent.getParcelableExtra<FOTemplateAdConfig>(FOTemplateAdConfig.ARG_BUNDLE)
                ?.languageAdConfig
        skipNavigateMain = intent.getBooleanExtra(EXTRA_SKIP_NAVIGATE_MAIN, false)

        // Legacy direct extras support if needed in future
        if (languageAdConfig == null && intent.hasExtra(LanguageAdConfig::class.java.name)) {
            languageAdConfig = intent.getParcelableExtra(LanguageAdConfig::class.java.name)
        }
    }

    private fun onDoneClick() {
        DataManager.user.firstOpen = false
        DataManager.user.chooseLang = true
        languageModel?.let {
            DataManager.user.language = it.languageCode
        }
        DataManager.saveData(this)
        Utility.setLocale(this)
        val resultIntent = Intent().apply {
            putExtra(EXTRA_SELECTED_LANGUAGE_CODE, languageModel?.languageCode)
            putExtra(EXTRA_SELECTED_LANGUAGE_NAME, languageModel?.name)
        }
        setResult(RESULT_LANGUAGE_SELECTED, resultIntent)

        if (skipNavigateMain) {
            finish()
        } else {
            val activityClass = DataManager.mainActivity
            if (activityClass != null) {
                val intent = Intent(this, activityClass)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            finish()
        }
    }

    private fun initLanguageData() {
        val languageList = buildLanguageList()
        if (languageList.isEmpty()) {
            return
        }

        languageModel = languageList.firstOrNull { it.isSelected } ?: languageList.first()

        val languageAdapter = LanguageAdapter(this, languageList, this)
        val selectedIndex = languageList.indexOfFirst { it.isSelected }.takeIf { it >= 0 } ?: 0
        languageAdapter.itemPosition = selectedIndex

        binding.recyclerView.adapter = languageAdapter
    }

    private fun buildLanguageList(): MutableList<LanguageModel> {
        // Use default language list since LanguageActivity has its own layout
        val list = buildDefaultLanguageList()

        if (list.isNotEmpty()) {
            val selectedIndex = list.indexOfFirst { it.isSelected }
            if (selectedIndex < 0) {
                val preferredCode = getPreferredLanguageCode()
                val preferredIndex = list.indexOfFirst { it.languageCode == preferredCode }
                if (preferredIndex >= 0) {
                    list.forEach { it.isSelected = false }
                    list[preferredIndex].isSelected = true
                } else {
                    list.first().isSelected = true
                }
            }
        }

        return list
    }

    private fun buildDefaultLanguageList(): MutableList<LanguageModel> {
        val languageList = mutableListOf<LanguageModel>()
        val userPreferred = getPreferredLanguageCode()

        for (i in 0 until languageCode.size) {
            val processing = languageCode[i]
            val languageModel = LanguageModel(i, countryName[i], languageCode[i], false)
            if (processing == userPreferred) {
                languageList.add(0, languageModel)
            } else {
                languageList.add(languageModel)
            }
        }

        if (languageList.isNotEmpty()) {
            languageList[0].isSelected = true
        }
        return languageList
    }

    private fun loadNativeAdIfNeeded() {
        val adConfig = languageAdConfig ?: run {
            binding.adContainer.visibility = View.GONE
            return
        }
        val container = binding.adContainer
        container.visibility = View.VISIBLE

        val nativeConfig = adConfig.nativeAdConfig
        val bannerConfig = adConfig.bannerAdConfig

        when {
            nativeConfig != null -> loadNativeAd(container, nativeConfig)
            bannerConfig != null -> loadBannerAd(container, bannerConfig)
            else -> container.visibility = View.GONE
        }
    }

    private fun loadNativeAd(container: ViewGroup, nativeConfig: NativeConfig) {
        val adUnit = nativeConfig.adUnitId
        if (adUnit.isEmpty()) {
            container.visibility = View.GONE
            return
        }

        val layoutId =
            if (nativeConfig.layoutId != 0) nativeConfig.layoutId else R.layout.native_default

        val nativeView = LayoutInflater.from(this).inflate(layoutId, container, false)
        val nativeAdView = nativeView as? NativeAdView ?: run {
            binding.adContainer.visibility = View.GONE
            return
        }

        val adapter: NativeAdViews = GenericNativeAdViews(nativeAdView)

        AdsController.loadNative(this, adUnit, container, adapter)
    }

    private fun loadBannerAd(container: ViewGroup, bannerConfig: BannerConfig) {
        val adUnit = bannerConfig.adUnitId
        if (adUnit.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        AdsController.loadBanner(
            this,
            adUnit,
            container,
            bannerConfig.sizeType
        )
    }

    private fun setHomeLocale(lang: String) {

        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        val context = baseContext.createConfigurationContext(config)
        applyOverrideConfiguration(config)

        finish()
    }

    override fun onSelectLanguage(languageModel: LanguageModel) {
        this.languageModel = languageModel
    }

    private fun getPreferredLanguageCode(): String {
        val userPreferred = DataManager.user.language

        if (userPreferred.isEmpty()) {
            return Locale.getDefault().language
        }

        return userPreferred
    }
}