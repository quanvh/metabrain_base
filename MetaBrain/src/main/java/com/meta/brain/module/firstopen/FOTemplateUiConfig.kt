package com.meta.brain.module.firstopen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Template UI configuration for First Open SDK Template 1
 * Contains language UI configurations
 */
@Parcelize
data class FOTemplateUiConfig(
    val languageUiConfig: LanguageUiConfig? = null,
) : Parcelable {

    companion object {
        const val ARG_BUNDLE = "FOTemplateUiConfig"
    }
}

