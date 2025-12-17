package com.meta.brain.module.language

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.nativead.NativeAdView
import android.widget.ImageView
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.meta.brain.R
import com.meta.brain.module.ads.AdsBanner
import com.meta.brain.module.ads.AdsController
import com.meta.brain.module.ads.AdsNative
import com.meta.brain.module.ads.GenericNativeAdViews
import com.meta.brain.module.ads.NativeAdViews
import com.meta.brain.module.base.BaseActivity
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager
import com.meta.brain.module.firstopen.BannerConfig
import com.meta.brain.module.firstopen.FOTemplateAdConfig
import com.meta.brain.module.firstopen.FOTemplateUiConfig
import com.meta.brain.module.firstopen.LanguageAdConfig
import com.meta.brain.module.firstopen.LanguageUiConfig
import com.meta.brain.module.firstopen.NativeConfig
import com.meta.brain.module.utils.Utility
import com.meta.brain.module.utils.invisible
import java.util.Locale

class LanguageActivity :
    BaseActivity(),
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
    private var languageUiConfig: LanguageUiConfig? = null
    private var skipNavigateMain: Boolean = false

    // Views - using findViewById
    private lateinit var imgBack: ImageView
    private lateinit var imgDone: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read configs first to get custom layout
        readConfigsEarly()

        // Set content view with layout from config or default
        val layoutToUse =
            if (languageUiConfig?.layoutId != null && languageUiConfig!!.layoutId != 0) {
                languageUiConfig!!.layoutId
            } else {
                R.layout.language_activity
            }
        setContentView(layoutToUse)

        // Initialize views using findViewById
        initViews()

        // Initialize UI
        initView()
    }

    private fun readConfigsEarly() {
        languageAdConfig =
            intent.getParcelableExtra<FOTemplateAdConfig>(FOTemplateAdConfig.ARG_BUNDLE)
                ?.languageAdConfig
        skipNavigateMain = intent.getBooleanExtra(EXTRA_SKIP_NAVIGATE_MAIN, false)

        // Legacy direct extras support if needed in future
        if (languageAdConfig == null && intent.hasExtra(LanguageAdConfig::class.java.name)) {
            languageAdConfig = intent.getParcelableExtra(LanguageAdConfig::class.java.name)
        }

        // Read UI config
        val templateUiConfig =
            intent.getParcelableExtra<FOTemplateUiConfig>(FOTemplateUiConfig.ARG_BUNDLE)
        languageUiConfig = templateUiConfig?.languageUiConfig

        // Legacy direct extras support if needed in future
        if (languageUiConfig == null && intent.hasExtra(LanguageUiConfig::class.java.name)) {
            languageUiConfig = intent.getParcelableExtra(LanguageUiConfig::class.java.name)
        }
    }

    private fun initViews() {
        imgBack = findViewById(R.id.imgBack)
        imgDone = findViewById(R.id.imgDone)
        recyclerView = findViewById(R.id.recyclerView)
        adContainer = findViewById(R.id.adContainer)
    }

    private fun initView() {
        readConfigs()

        imgBack.invisible()
        imgBack.setOnClickListener { finish() }

        imgDone.setOnClickListener {
            FirebaseManager.sendLog("language_click_done", null)
            onDoneClick()
        }
        initLanguageData()
        loadNativeAdIfNeeded()
        FirebaseManager.sendLog("language_viewed", null)
    }

    private fun readConfigs() {
        // Configs are already read in readConfigsEarly(), but keep this for compatibility
        // Only read if not already read
        if (languageAdConfig == null) {
            languageAdConfig =
                intent.getParcelableExtra<FOTemplateAdConfig>(FOTemplateAdConfig.ARG_BUNDLE)
                    ?.languageAdConfig
            skipNavigateMain = intent.getBooleanExtra(EXTRA_SKIP_NAVIGATE_MAIN, false)

            // Legacy direct extras support if needed in future
            if (languageAdConfig == null && intent.hasExtra(LanguageAdConfig::class.java.name)) {
                languageAdConfig = intent.getParcelableExtra(LanguageAdConfig::class.java.name)
            }
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

        // Use custom item layout if provided in UI config
        val itemLayoutId = languageUiConfig?.itemLayoutId
        val languageAdapter = if (itemLayoutId != null && itemLayoutId != 0) {
            LanguageAdapter(this, languageList, this, itemLayoutId)
        } else {
            LanguageAdapter(this, languageList, this)
        }
        val selectedIndex = languageList.indexOfFirst { it.isSelected }.takeIf { it >= 0 } ?: 0
        languageAdapter.itemPosition = selectedIndex

        recyclerView.adapter = languageAdapter
    }

    private fun buildLanguageList(): MutableList<LanguageModel> {
        // Use language list from UI config if provided, otherwise use default
        val list =
            if (languageUiConfig?.listLanguage != null && languageUiConfig!!.listLanguage.isNotEmpty()) {
                languageUiConfig!!.listLanguage.toMutableList()
            } else {
                buildDefaultLanguageList()
            }

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
            adContainer.visibility = View.GONE
            return
        }
        adContainer.visibility = View.VISIBLE

        val nativeConfig = adConfig.nativeAdConfig
        val bannerConfig = adConfig.bannerAdConfig

        when {
            nativeConfig != null -> loadNativeAd(adContainer, nativeConfig)
            bannerConfig != null -> loadBannerAd(adContainer, bannerConfig)
            else -> adContainer.visibility = View.GONE
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
            adContainer.visibility = View.GONE
            return
        }

        val adapter: NativeAdViews = GenericNativeAdViews(nativeAdView)
        val adNative = AdsNative()

        adNative.loadNative(
            context = this,
            adUnit = adUnit,
            views = adapter,
            onEvent = null,
            container = container
        )
    }

    private fun loadBannerAd(container: ViewGroup, bannerConfig: BannerConfig) {
        val adUnit = bannerConfig.adUnitId
        if (adUnit.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        val adBanner = AdsBanner()
        adBanner.loadBanner(
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
        FirebaseManager.sendLog("language_selected_click", Bundle().apply {
            putString("language_code", languageModel.languageCode)
        })
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