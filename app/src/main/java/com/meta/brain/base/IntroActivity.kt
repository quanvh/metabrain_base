package com.meta.brain.base

import com.meta.brain.base.databinding.IntroActivityBinding
import com.meta.brain.module.ads.AdEvent
import com.meta.brain.module.ads.AdsController
import com.meta.brain.module.base.DataBindActivity

class IntroActivity : DataBindActivity<IntroActivityBinding>(R.layout.intro_activity) {
    override fun initView(){
        binding.btnStart.setOnClickListener {
            AdsController.showInter(this,object : AdEvent(){
                override fun onComplete() {
                    startHome()
                }
            })
        }
    }

    fun startHome(){
//        startActivity(Intent(this, HomeActivity::class.java))
//        finish()
    }
}