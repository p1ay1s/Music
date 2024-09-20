package com.niki.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment

abstract class BaseFragment<VB : ViewDataBinding> : Fragment(),
    BaseInterface<VB> {

    protected lateinit var mBinding: VB

    /**
     * 函数内可直接引用控件id
     * - " myTextView.text = myString "
     * - " binding = this@initBinding "
     */
    abstract fun VB.initBinding()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = getViewBinding(inflater, container)
        return mBinding.root
    }

    /**
     * fragment的super方法包含了initBinding, 可以据此安排代码
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.initBinding()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放, 防止内存泄漏
        if (::mBinding.isInitialized) {
            mBinding.unbind()
        }
    }
}