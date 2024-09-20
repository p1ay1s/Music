package com.niki.base.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import com.niki.base.BaseInterface

abstract class BaseActivity<VB : ViewDataBinding> : AppCompatActivity(),
    BaseInterface<VB> {

    /**
     * 函数内可直接引用控件id
     * 可以直接当做 onCreate 使用
     * - " myTextView.text = myString "
     * - " binding = this@initBinding "
     */
    abstract fun VB.initBinding()

    protected val mBinding: VB by lazy(mode = LazyThreadSafetyMode.NONE) {
        getViewBinding(layoutInflater)
    }

    /**
     * activity的super方法包含了initBinding, 可以据此安排代码
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        mBinding.initBinding()
    }

    override fun onDestroy() {
        super.onDestroy()
        mBinding.unbind()
    }
}