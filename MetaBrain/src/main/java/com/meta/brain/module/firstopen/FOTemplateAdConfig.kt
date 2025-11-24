package com.meta.brain.module.firstopen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Template ad configuration for First Open SDK Template 1
 * Contains language and onboarding ad configurations
 */
@Parcelize
data class FOTemplateAdConfig(
    val languageAdConfig: LanguageAdConfig,
) : Parcelable {

    companion object {
        const val ARG_BUNDLE = "FOTemplateAdConfig"
    }
}

