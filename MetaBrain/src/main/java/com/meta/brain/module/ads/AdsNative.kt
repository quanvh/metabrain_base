package com.meta.brain.module.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.*
import com.meta.brain.R
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
                nativeAd.setOnPaidEventListener { adValue -> AdsController.logAdRevenue(adValue,nativeAd.responseInfo) }
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

        nativeAdView.mediaView = nativeBinding.adMedia

        nativeAdView.headlineView = nativeBinding.adHeadline
        nativeBinding.adHeadline?.let { headlineView ->
            headlineView.text = nativeAd.headline
        }

        nativeAd.mediaContent?.let { mediaContent ->
            nativeBinding.adMedia?.mediaContent = mediaContent
        }

        nativeAdView.bodyView = nativeBinding.adBody
        nativeBinding.adBody?.let { bodyView ->
            if (nativeAd.body == null) {
                bodyView.visibility = View.INVISIBLE
            } else {
                bodyView.visibility = View.VISIBLE
                bodyView.text = nativeAd.body
            }
        }

        nativeAdView.callToActionView = nativeBinding.adCallToAction
        nativeBinding.adCallToAction?.let { ctaView ->
            if (nativeAd.callToAction == null) {
                ctaView.visibility = View.INVISIBLE
            } else {
                ctaView.visibility = View.VISIBLE
                ctaView.text = nativeAd.callToAction
            }
        }

        nativeAdView.iconView = nativeBinding.adAppIcon
        nativeBinding.adAppIcon?.let { iconView ->
            if (nativeAd.icon == null) {
                iconView.visibility = View.GONE
            } else {
                iconView.setImageDrawable(nativeAd.icon?.drawable)
                iconView.visibility = View.VISIBLE
            }
        }

        nativeAdView.priceView = nativeBinding.adPrice
        nativeBinding.adPrice?.let { priceView ->
            if (nativeAd.price == null) {
                priceView.visibility = View.INVISIBLE
            } else {
                priceView.visibility = View.VISIBLE
                priceView.text = nativeAd.price
            }
        }

        nativeAdView.starRatingView = nativeBinding.adStars
        nativeBinding.adStars?.let { starsView ->
            if (nativeAd.starRating == null) {
                starsView.visibility = View.INVISIBLE
            } else {
                starsView.rating = nativeAd.starRating!!.toFloat()
                starsView.visibility = View.VISIBLE
            }
        }

        nativeAdView.storeView = nativeBinding.adStore
        nativeBinding.adStore?.let { storeView ->
            if (nativeAd.store == null) {
                storeView.visibility = View.INVISIBLE
            } else {
                storeView.visibility = View.VISIBLE
                storeView.text = nativeAd.store
            }
        }

        nativeAdView.advertiserView = nativeBinding.adAdvertiser
        nativeBinding.adAdvertiser?.let { advertiserView ->
            if (nativeAd.advertiser == null) {
                advertiserView.visibility = View.INVISIBLE
            } else {
                advertiserView.text = nativeAd.advertiser
                advertiserView.visibility = View.VISIBLE
            }
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
    val adMedia: MediaView?
    val adHeadline: TextView?
    val adBody: TextView?
    val adCallToAction: Button?
    val adAppIcon: ImageView?
    val adPrice: TextView?
    val adStars: RatingBar?
    val adStore: TextView?
    val adAdvertiser: TextView?
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

/**
 * Generic adapter: dùng cho mọi layout native miễn là các view có id chuẩn:
 *  - ad_media, ad_headline, ad_body, ad_call_to_action, ad_app_icon,
 *    ad_price, ad_stars, ad_store, ad_advertiser
 */
class GenericNativeAdViews(
    private val rootView: NativeAdView
) : NativeAdViews {
    override val root get() = rootView
    override val adMedia: MediaView? get() = rootView.findViewById(R.id.ad_media)
    override val adHeadline: TextView? get() = rootView.findViewById(R.id.ad_headline)
    override val adBody: TextView? get() = rootView.findViewById(R.id.ad_body)
    override val adCallToAction: Button? get() = rootView.findViewById(R.id.ad_call_to_action)
    override val adAppIcon: ImageView? get() = rootView.findViewById(R.id.ad_app_icon)
    override val adPrice: TextView? get() = rootView.findViewById(R.id.ad_price)
    override val adStars: RatingBar? get() = rootView.findViewById(R.id.ad_stars)
    override val adStore: TextView? get() = rootView.findViewById(R.id.ad_store)
    override val adAdvertiser: TextView? get() = rootView.findViewById(R.id.ad_advertiser)
}