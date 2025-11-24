package com.meta.brain.module.firstopen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Language ad configuration for First Open SDK Template 1
 * Contains native ad configurations for language selection screens
 */
@Parcelize
data class LanguageAdConfig(
    val nativeAdConfig: NativeConfig? = null,
    val bannerAdConfig: BannerConfig? = null,
) : Parcelable

