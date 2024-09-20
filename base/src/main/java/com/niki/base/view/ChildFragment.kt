package com.niki.base

import androidx.databinding.ViewDataBinding

abstract class ChildFragment<VB : ViewDataBinding> : BaseFragment<VB>() {

    protected fun switchToFragment(index: String) = kotlin.runCatching {
        (parentFragment as ContainerFragment).switchToFragment(index)
    }
}