package com.meta.brain.module.firstopen

import android.os.Parcelable
import androidx.annotation.LayoutRes
import com.meta.brain.module.language.LanguageModel
import kotlinx.parcelize.Parcelize

/**
 * Language UI configuration for First Open SDK Template 1
 * Contains layout IDs, language list, and selected/tooltip language items
 */
@Parcelize
data class LanguageUiConfig(
    @LayoutRes val layoutId: Int,
    @LayoutRes val itemLayoutId: Int,
    val listLanguage: List<LanguageModel>,
) : Parcelable

