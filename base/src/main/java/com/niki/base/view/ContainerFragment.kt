package com.niki.base

import android.content.Context
import androidx.fragment.app.Fragment
import com.niki.base.utils.FragmentManagerHelper

open class ContainerFragment(private val fragmentMap: LinkedHashMap<String, Fragment>) :
    Fragment() {

    protected var helper: FragmentManagerHelper? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        helper = FragmentManagerHelper(
            childFragmentManager,
            R.id.frameLayout_Container,
            fragmentMap
        )
    }

    fun switchToFragment(index: String) {
        helper?.switchToFragment(index)
    }
}