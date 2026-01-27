package com.meta.brain.module.base

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.viewbinding.ViewBinding

abstract class DataBindActivity<VB : ViewBinding>(@LayoutRes val layout: Int): BaseActivity(){

    open val binding by lazy {
        DataBindingUtil.setContentView(this, layout) as VB
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    abstract fun initView()

}