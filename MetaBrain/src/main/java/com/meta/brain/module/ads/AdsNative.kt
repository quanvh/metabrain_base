package com.meta.brain.module.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.*
import com.meta.brain.databinding.NativeDefaultBinding
import com.meta.brain.module.base.MetaBrainApp
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager

class AdsNative {
    companion object {
        private const val TAG = "[AdsNative]"
    }

    private var currentNativeAd: NativeAd? = null

    fun loadNative(context: Context, adUnit: String, views: NativeAdViews, onEvent: AdEvent?) {
        if(FirebaseManager.rc.useAds && !DataManager.user.removeAds) {
            if (MetaBrainApp.debug) {
                Log.d(TAG, "Native Ad call, id: $adUnit")
            }

            FirebaseManager.sendLog("native_call",null)
            val builder = AdLoader.Builder(context, adUnit)
            builder.forNativeAd { nativeAd ->
                currentNativeAd?.destroy()
                currentNativeAd = nativeAd
                populateNativeAdView(nativeAd, views)
            }

            val videoOptions =
                VideoOptions.Builder()
                    .setStartMuted(false)
                    .build()

            val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()

            builder.withNativeAdOptions(adOptions)

            val adLoader = builder.withAdListener(
                object : AdListener() {
                    override fun onAdLoaded() {
                        if (MetaBrainApp.debug) {
                            Log.d(TAG, "Native Ad loaded, id: $adUnit")
                        }
                        FirebaseManager.sendLog("native_loaded",null)
                        onEvent?.onLoaded()
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        FirebaseManager.sendLog("native_load_fail",null)
                        onEvent?.onLoadFail()
                        if (MetaBrainApp.debug) {
                            Log.d(TAG, "Native Ad load failed: " + loadAdError.message)

                            val error =
                                """domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}""""
                            Toast.makeText(
                                context as Activity,
                                "Failed to load native ad with error $error",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }


                    override fun onAdImpression() {
                        if (MetaBrainApp.debug) {
                            Log.d(TAG, "Native Ad impress, id: $adUnit")
                        }
                        FirebaseManager.sendLog("native_impress",null)
                        onEvent?.onImpress()
                    }

                    override fun onAdClicked() {
                        if (MetaBrainApp.debug) {
                            Log.d(TAG, "Native Ad clicked, id: $adUnit")
                        }
                        FirebaseManager.sendLog("native_click",null)
                        onEvent?.onClick()
                    }
                }).build()

            adLoader.loadAd(AdRequest.Builder().build())
        } else {
            onEvent?.onLoaded()
        }
    }

    private fun populateNativeAdView(nativeAd: NativeAd, nativeBinding: NativeAdViews) {
        val nativeAdView = nativeBinding.root

        // Set the media view.
        nativeAdView.mediaView = nativeBinding.adMedia

        // Set other ad assets.
        nativeAdView.headlineView = nativeBinding.adHeadline
        nativeAdView.bodyView = nativeBinding.adBody
        nativeAdView.callToActionView = nativeBinding.adCallToAction
        nativeAdView.iconView = nativeBinding.adAppIcon
        nativeAdView.priceView = nativeBinding.adPrice
        nativeAdView.starRatingView = nativeBinding.adStars
        nativeAdView.storeView = nativeBinding.adStore
        nativeAdView.advertiserView = nativeBinding.adAdvertiser

        nativeBinding.adHeadline.text = nativeAd.headline
        nativeAd.mediaContent?.let { nativeBinding.adMedia.mediaContent = it }

        if (nativeAd.body == null) {
            nativeBinding.adBody.visibility = View.INVISIBLE
        } else {
            nativeBinding.adBody.visibility = View.VISIBLE
            nativeBinding.adBody.text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            nativeBinding.adCallToAction.visibility = View.INVISIBLE
        } else {
            nativeBinding.adCallToAction.visibility = View.VISIBLE
            nativeBinding.adCallToAction.text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            nativeBinding.adAppIcon.visibility = View.GONE
        } else {
            nativeBinding.adAppIcon.setImageDrawable(nativeAd.icon?.drawable)
            nativeBinding.adAppIcon.visibility = View.VISIBLE
        }

        if (nativeAd.price == null) {
            nativeBinding.adPrice.visibility = View.INVISIBLE
        } else {
            nativeBinding.adPrice.visibility = View.VISIBLE
            nativeBinding.adPrice.text = nativeAd.price
        }

        if (nativeAd.store == null) {
            nativeBinding.adStore.visibility = View.INVISIBLE
        } else {
            nativeBinding.adStore.visibility = View.VISIBLE
            nativeBinding.adStore.text = nativeAd.store
        }

        if (nativeAd.starRating == null) {
            nativeBinding.adStars.visibility = View.INVISIBLE
        } else {
            nativeBinding.adStars.rating = nativeAd.starRating!!.toFloat()
            nativeBinding.adStars.visibility = View.VISIBLE
        }

        if (nativeAd.advertiser == null) {
            nativeBinding.adAdvertiser.visibility = View.INVISIBLE
        } else {
            nativeBinding.adAdvertiser.text = nativeAd.advertiser
            nativeBinding.adAdvertiser.visibility = View.VISIBLE
        }

        nativeAdView.setNativeAd(nativeAd)

        /* for handle video end
        val mediaContent = nativeAd.mediaContent
        val vc = mediaContent?.videoController

        if (vc != null && mediaContent.hasVideoContent()) {
            vc.videoLifecycleCallbacks =
                object : VideoController.VideoLifecycleCallbacks() {
                    override fun onVideoEnd() {
                        super.onVideoEnd()
                    }
                }
        }
        */
    }

}

interface NativeAdViews {
    val root: NativeAdView
    val adMedia: MediaView
    val adHeadline: TextView
    val adBody: TextView
    val adCallToAction: Button
    val adAppIcon: ImageView
    val adPrice: TextView
    val adStars: RatingBar
    val adStore: TextView
    val adAdvertiser: TextView
}

class NativeDefaultBindingAdapter(
    private val b: NativeDefaultBinding
) : NativeAdViews {
    override val root get() = b.root
    override val adMedia get() = b.adMedia
    override val adHeadline get() = b.adHeadline
    override val adBody get() = b.adBody
    override val adCallToAction get() = b.adCallToAction
    override val adAppIcon get() = b.adAppIcon
    override val adPrice get() = b.adPrice
    override val adStars get() = b.adStars
    override val adStore get() = b.adStore
    override val adAdvertiser get() = b.adAdvertiser
}