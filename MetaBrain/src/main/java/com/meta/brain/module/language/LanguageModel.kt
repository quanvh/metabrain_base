package com.meta.brain.module.language

class LanguageModel (
    var id: Int,
    var name: String,
    var languageCode: String,
    var isSelected: Boolean
) {
    override fun toString(): String {
        return "LanguageModel(id=$id, name='$name', languageCode='$languageCode', isSelected=$isSelected)"
    }
}