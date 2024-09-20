package com.niki.base.view

import androidx.fragment.app.Fragment
import com.niki.base.databinding.FragmentContainerBinding
import com.niki.base.view.ui.FragmentControllerView

open class ContainerFragment(private val fragmentMap: LinkedHashMap<String, Fragment>) :
    BaseFragment<FragmentContainerBinding>() {

    protected var controllerView: FragmentControllerView? = null
    override fun FragmentContainerBinding.initBinding() {
        fragmentControllerViewContainer.run {
            controllerView = this
            setFragmentManager(childFragmentManager)
            submitMap(fragmentMap)
            init()
        }
    }

    fun switchToFragment(index: String) {
        controllerView?.switchToFragment(index)
    }
}