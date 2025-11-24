package com.meta.brain.module.firstopen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NativeConfig(
    val adUnitId: String,
    val layoutId: Int,
) : Parcelable