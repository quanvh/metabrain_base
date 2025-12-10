package com.meta.brain.module.utils // đổi theo project của bạn

import android.util.Log
import com.meta.brain.module.base.MetaBrainApp

private const val TAG = "MetaBrain"

fun debugLog(
    tag: String = "MetaBrain",
    message: String,
    block: () -> Unit = {}
) {
    if (MetaBrainApp.debug) {
        Log.d(tag, message)
        block()
    }
}
