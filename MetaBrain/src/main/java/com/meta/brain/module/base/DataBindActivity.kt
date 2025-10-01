package com.meta.brain.module.base

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

abstract class DataBindActivity<DB : ViewDataBinding>(@LayoutRes val layout: Int): BaseActivity(){

    val binding by lazy {
        DataBindingUtil.setContentView(this, layout) as DB
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBindingLifecycleOwner()
        initView()
    }

    abstract fun initView()

    private fun setupBindingLifecycleOwner() {
        binding.lifecycleOwner = this
    }
}