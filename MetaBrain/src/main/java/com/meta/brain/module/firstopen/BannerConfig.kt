package com.meta.brain.module.firstopen

import android.os.Parcelable
import com.meta.brain.module.ads.BannerSizeType
import kotlinx.parcelize.Parcelize

@Parcelize
data class BannerConfig(
    val adUnitId: String,
    val sizeType: BannerSizeType = BannerSizeType.BANNER,
) : Parcelable

