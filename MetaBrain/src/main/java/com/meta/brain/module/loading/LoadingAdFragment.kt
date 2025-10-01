package com.meta.brain.module.loading

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.core.graphics.drawable.toDrawable
import com.meta.brain.R
import com.meta.brain.module.ads.AdEvent
import com.meta.brain.module.ads.AdsController
import com.meta.brain.module.base.MetaBrainApp

class LoadingAdFragment: DialogFragment() {
    //Demo call
//    LoadingAdFragment.newInstance(LoadingAdFragment.Companion.AdType.INTERSTITIAL)
//    .setOnDone { -> Log.d(TAG, "==== close fragment") }
//    .show(supportFragmentManager, TAG)


    companion object {
        const val TAG = "[LoadingAdsFragment]"

        private const val ARG_AD_TYPE = "arg_ad_type"
        private var isResume = false

        fun newInstance(adType: AdType, resume: Boolean): LoadingAdFragment {
            return LoadingAdFragment().apply {
                isCancelable = false
                isResume = resume
                arguments = Bundle().apply {
                    putString(ARG_AD_TYPE, adType.name)
                }
            }
        }

        enum class AdType {
            INTERSTITIAL,
            REWARDED,
            APP_OPEN
        }
    }

    private var onDone: (() -> Unit)? = null

    fun setOnDone(block: () -> Unit): LoadingAdFragment {
        onDone = block
        return this
    }

    private val adType: AdType by lazy {
        val typeName = arguments?.getString(ARG_AD_TYPE) ?: AdType.INTERSTITIAL.name
        AdType.valueOf(typeName)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND) // tự dim bằng layout
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.loading_ad_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (adType) {
            AdType.INTERSTITIAL -> {
                if(isResume) {
                    Log.d(TAG, "==== load inter resume")
                    AdsController.loadInterResume(requireContext(), object : AdEvent() {
                        override fun onLoaded() {
                            AdsController.showInterResume(requireActivity(), object : AdEvent() {
                                override fun onComplete() = closeLoadAd()
                                override fun onShowFail() = closeLoadAd()
                            })
                        }

                        override fun onLoadFail() = closeLoadAd()
                    })
                } else {
                    AdsController.loadInter(requireContext(), object : AdEvent() {
                        override fun onLoaded() {
                            AdsController.showInter(requireActivity(), object : AdEvent() {
                                override fun onComplete() = closeLoadAd()
                                override fun onShowFail() = closeLoadAd()
                            })
                        }

                        override fun onLoadFail() = closeLoadAd()
                    })
                }
            }

            AdType.REWARDED -> {
                AdsController.loadReward(requireContext(), object : AdEvent() {
                    override fun onLoaded() {
                        AdsController.showReward(requireActivity(), object : AdEvent() {
                            override fun onComplete() = closeLoadAd()
                            override fun onShowFail() = closeLoadAd()
                        })
                    }

                    override fun onLoadFail() = closeLoadAd()
                })
            }

            AdType.APP_OPEN -> {
                AdsController.loadOpenAdResume(requireContext(), object : AdEvent() {
                    override fun onLoaded() {
                        AdsController.showOpenAdResume(requireActivity(), object : AdEvent() {
                            override fun onComplete() = closeLoadAd()
                            override fun onShowFail() = closeLoadAd()
                        })
                    }

                    override fun onLoadFail() = closeLoadAd()
                })
            }
        }
    }

    fun closeLoadAd(){
        if(MetaBrainApp.debug){
            Log.d(TAG, "Close Loading ads fragment")
        }
        if (isAdded) dismissAllowingStateLoss()
        onDone?.invoke()
    }
}