package com.meta.brain.module.language

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class LanguageModel (
    var id: Int,
    var name: String,
    var languageCode: String,
    var isSelected: Boolean
): Parcelable {
    override fun toString(): String {
        return "LanguageModel(id=$id, name='$name', languageCode='$languageCode', isSelected=$isSelected)"
    }
}