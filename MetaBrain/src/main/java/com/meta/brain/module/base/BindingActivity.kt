package com.meta.brain.module.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.viewbinding.ViewBinding

abstract class BindingActivity<T : ViewBinding> : BaseActivity() {
    protected lateinit var binding: T
        private set

    protected abstract fun inflateBinding(inflater: LayoutInflater): T

    final override fun createContentView(savedInstanceState: Bundle?): View {
        binding = inflateBinding(layoutInflater)
        return binding.root
    }
}