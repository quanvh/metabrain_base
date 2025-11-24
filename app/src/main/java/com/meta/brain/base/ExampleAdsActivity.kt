package com.meta.brain.base

import android.content.Intent
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import com.meta.brain.base.databinding.BannerActivityBinding
import com.meta.brain.module.ads.AdsController
import com.meta.brain.module.ads.BannerSizeType
import com.meta.brain.module.base.DataBindActivity
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firstopen.BannerConfig
import com.meta.brain.module.firstopen.FOTemplateAdConfig
import com.meta.brain.module.firstopen.LanguageAdConfig
import com.meta.brain.module.firstopen.NativeConfig
import com.meta.brain.module.language.LanguageActivity

class ExampleAdsActivity : DataBindActivity<BannerActivityBinding>(R.layout.banner_activity) {

    private val languageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == LanguageActivity.RESULT_LANGUAGE_SELECTED) {
                val code =
                    result.data?.getStringExtra(LanguageActivity.EXTRA_SELECTED_LANGUAGE_CODE)
                val name =
                    result.data?.getStringExtra(LanguageActivity.EXTRA_SELECTED_LANGUAGE_NAME)
                Log.d("BannerActivity", "Language selected via callback: $name($code)")
            }
        }

    override fun initView() {
        Log.d("BannerActivity", "language: ${DataManager.user.language}")
        loadBannerAd()
        binding.btnReloadBanner.setOnClickListener {
            loadBannerAd()
        }
        binding.btnOpenNative.setOnClickListener {
            startActivity(Intent(this, NativeAdsActivity::class.java))
        }
        binding.btnOpenInter.setOnClickListener {
            startActivity(Intent(this, DemoInterstitialActivity::class.java))
        }
        binding.btnLanguage.setOnClickListener {
//            openLanguageWithNativeAd()
            openLanguageWithBannerAd()
        }
    }

    private fun openLanguageWithNativeAd() {
        val intent = Intent(this, LanguageActivity::class.java).apply {
            putExtra(FOTemplateAdConfig.ARG_BUNDLE, createNativeAdConfig())
            putExtra(LanguageActivity.EXTRA_SKIP_NAVIGATE_MAIN, true)
        }
        languageLauncher.launch(intent)
    }

    private fun openLanguageWithBannerAd() {
        val intent = Intent(this, LanguageActivity::class.java).apply {
            putExtra(FOTemplateAdConfig.ARG_BUNDLE, createBannerAdConfig())
            putExtra(LanguageActivity.EXTRA_SKIP_NAVIGATE_MAIN, true)
        }
        languageLauncher.launch(intent)
    }

    private fun createNativeAdConfig(): FOTemplateAdConfig {
        return FOTemplateAdConfig(
            languageAdConfig = LanguageAdConfig(
                nativeAdConfig = NativeConfig(
                    adUnitId = getString(com.meta.brain.R.string.native_home),
                    layoutId = com.meta.brain.R.layout.native_default
                )
            )
        )
    }

    private fun createBannerAdConfig(): FOTemplateAdConfig {
        return FOTemplateAdConfig(
            languageAdConfig = LanguageAdConfig(
                bannerAdConfig = BannerConfig(
                    adUnitId = getString(com.meta.brain.R.string.banner_default),
                )
            )
        )
    }

    private fun loadBannerAd() {
        AdsController.loadBanner(
            this,
            getString(com.meta.brain.R.string.banner_default),
            binding.bannerContainer,
            BannerSizeType.LARGE_BANNER
        )
    }
}
