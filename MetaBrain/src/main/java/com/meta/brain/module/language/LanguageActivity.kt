package com.meta.brain.module.language

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.material3.DatePicker
import com.meta.brain.R
import com.meta.brain.databinding.LanguageActivityBinding
//import com.meta.brain.file.recovery.IntroActivity
//import com.meta.brain.file.recovery.HomeActivity
import com.meta.brain.module.base.DataBindActivity
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.utils.PrefUtil
import com.meta.brain.module.utils.Utility
import com.meta.brain.module.utils.invisible
import com.meta.brain.module.utils.visible
import java.util.Locale

class LanguageActivity : DataBindActivity<LanguageActivityBinding>(R.layout.language_activity),LanguageAdapter.LanguageAdapterCallBack{
    companion object {

        val countryName = mutableListOf(
            "English", "Indonesia", "Portuguese", "Spanish", "India", "Turkey", "France", "Vietnamese", "Russian"
        )

        val languageCode = mutableListOf(
            "en", "in", "pt", "es", "hi", "tr", "fr", "vi", "ru"
        )
    }
    private var languageModel: LanguageModel? = null

    override fun initView() {

        binding.imgBack.invisible()
        binding.imgBack.setOnClickListener { finish() }

        binding.imgDone.setOnClickListener {
            onDoneClick()
        }
        initLanguageData()
    }

    private fun onDoneClick() {
        DataManager.user.firstOpen = false
        DataManager.user.chooseLang = true
        if (languageModel != null) {
            DataManager.user.language = languageModel!!.languageCode
        }
        DataManager.saveData(this)
        Utility.setLocale(this)
        val activityClass = DataManager.mainActivity
        if (activityClass != null) {
            val intent = Intent(this, activityClass)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            finish()
        } else {
            throw IllegalStateException("MainActivity init first!")
        }
    }

    private fun initLanguageData() {
        val languageList = mutableListOf<LanguageModel>()
        val userPreferred = getPreferredLanguageCode()

        for (i in 0 until languageCode.size) {
            val processing = languageCode[i]
            val languageModel = LanguageModel(i, countryName[i], languageCode[i], false)
            if (processing == userPreferred) {
                languageList.add(0, languageModel)
            } else {
                languageList.add(languageModel)
            }
        }

        languageList[0].isSelected = true
        val languageAdapter = LanguageAdapter(this, languageList)

        binding.recyclerView.adapter = languageAdapter
    }

    private fun setHomeLocale(lang: String) {

        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        val context = baseContext.createConfigurationContext(config)
        applyOverrideConfiguration(config)

//        val refresh = Intent(context, HomeActivity::class.java)
//        refresh.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
//                Intent.FLAG_ACTIVITY_CLEAR_TASK or
//                Intent.FLAG_ACTIVITY_NEW_TASK

//        startActivity(refresh)
        finish()
    }

    override fun onSelectLanguage(languageModel: LanguageModel) {
        this.languageModel = languageModel
    }

    private fun getPreferredLanguageCode(): String {
        val userPreferred = DataManager.user.language

        if (userPreferred.isEmpty()) {
            return Locale.getDefault().language
        }

        return userPreferred
    }
}