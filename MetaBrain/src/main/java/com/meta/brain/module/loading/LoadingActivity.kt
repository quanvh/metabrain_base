package com.meta.brain.module.loading

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.meta.brain.R
//import com.meta.brain.file.recovery.IntroActivity
import com.meta.brain.module.ads.AdEvent
import com.meta.brain.module.ads.AdsController
import com.meta.brain.module.ads.UMP
import com.meta.brain.module.base.BaseActivity
import com.meta.brain.module.data.DataManager
import com.meta.brain.module.firebase.FirebaseManager
import com.meta.brain.module.firebase.RemoteEvent
import com.meta.brain.module.language.LanguageActivity
import com.meta.brain.module.utils.Utility
import com.meta.brain.module.utils.showUpdateDialog
import kotlinx.coroutines.*

class LoadingActivity : BaseActivity() {

    private lateinit var ump: UMP

    private var loadingJob: Job? = null
    companion object{
        const val TAG = "[LoadingActivity]"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.loading_activity);

        DataManager.init(this)

        FirebaseManager.initFirebase(this, object : RemoteEvent(){
            override fun onFetched() {
                ump = UMP.getInstance(this@LoadingActivity)
                ump.gatherConsent(this@LoadingActivity) { consentError ->
                    if (consentError != null) {
                        Log.w(TAG, "${consentError.errorCode}: ${consentError.message}")
                    }

                    if (ump.canRequestAds) {
                        AdsController.initAdmob(this@LoadingActivity)
                    }
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        Utility.setLocale(this)
        DataManager.user.recoverCount = 0
        checkAds()
    }

    override fun onResume() {
        super.onResume()
    }

    private val totalTimeWait : Int = 30
    private var timeWait: Int = 0
    private fun checkAds() {
        loadingJob = lifecycleScope.launch {
            while (isActive) {
                if (AdsController.isOpenReady() || timeWait >= totalTimeWait) {
                    loadingJob?.cancel()
                    loadingJob = null


                    if(FirebaseManager.rc.useAds) {
                        if(FirebaseManager.rc.useInterOpen){
                            AdsController.showInterOpen(this@LoadingActivity,object : AdEvent(){
                                override fun onComplete() {
                                    initApp()
                                }
                            })
                        } else if(FirebaseManager.rc.useOpenSplash) {
                            AdsController.showOpenAd(
                                this@LoadingActivity,
                                object : AdEvent() {
                                    override fun onComplete() {
                                        initApp()
                                    }
                                })
                        } else {
                            initApp()
                        }
                    } else {
                        initApp()
                    }
                } else {
                    timeWait++
                }
                delay(1000)
            }
        }
    }

    private fun initApp(){
        checkUpdate()
    }


    private fun checkUpdate(){
        var currentCode = 0L
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            currentCode = packageInfo.longVersionCode
        } else {
            currentCode = packageInfo.versionCode.toLong()
        }
        if(FirebaseManager.appVersion.isForce && FirebaseManager.appVersion.versionCode > currentCode){
            showUpdateDialog()
        } else {
            startMain()
        }
    }


    private var isStartMain = false

    private fun startMain() {
        if (isStartMain) {
            return
        }
        isStartMain = true
        if (!isDestroyed) {
            if (DataManager.user.firstOpen) {
                val intent = Intent(this,LanguageActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                intent.putExtra("type", LanguageActivity.LANGUAGE_FIRST_OPEN)

                startActivity(intent)
            } else {
//                val intent = Intent(this, IntroActivity::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//
//                startActivity(intent)
            }
        }
    }
}

