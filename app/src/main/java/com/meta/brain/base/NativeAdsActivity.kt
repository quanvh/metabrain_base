package com.meta.brain.base

import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.gms.ads.nativead.NativeAdView
import com.meta.brain.base.databinding.NativeActivityBinding
import com.meta.brain.module.ads.AdEvent
import com.meta.brain.module.ads.AdsController
import com.meta.brain.module.ads.GenericNativeAdViews
import com.meta.brain.module.base.DataBindActivity

class NativeAdsActivity :
    DataBindActivity<NativeActivityBinding>(R.layout.native_activity) {

    private val nativeAdapter by lazy {
        val nativeView = layoutInflater.inflate(
            R.layout.native_no_media_default,
            null
        ) as NativeAdView
        GenericNativeAdViews(nativeView)
    }

    override fun initView() {
        loadNativeAd()
        binding.btnReloadNative.setOnClickListener {
            loadNativeAd()
        }
    }

    private fun loadNativeAd() {
        AdsController.loadNative(
            this,
            getString(com.meta.brain.R.string.native_home),
            binding.nativeContainer,
            nativeAdapter,
            object : AdEvent(){
                override fun onLoaded() {
                    super.onLoaded()
                    Log.d("NativeAdsActivity", "onLoaded")
                }
            }
        )
    }

    override fun createContentView(savedInstanceState: Bundle?): View {
       return NativeActivityBinding.inflate(layoutInflater).root
    }

    override fun updateUI(savedInstanceState: Bundle?) {
    }
}

