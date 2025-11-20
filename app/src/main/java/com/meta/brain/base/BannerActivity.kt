package com.meta.brain.base

import android.content.Intent
import com.meta.brain.base.databinding.BannerActivityBinding
import com.meta.brain.module.ads.AdsController
import com.meta.brain.module.ads.BannerSizeType
import com.meta.brain.module.base.DataBindActivity

class BannerActivity : DataBindActivity<BannerActivityBinding>(R.layout.banner_activity) {

    override fun initView() {
        loadBannerAd()
        binding.btnReloadBanner.setOnClickListener {
            loadBannerAd()
        }
        binding.btnOpenNative.setOnClickListener {
            startActivity(Intent(this, NativeAdsActivity::class.java))
        }
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

