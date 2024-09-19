package com.niki.utils.base.utils

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * 功能为简单的fragment切换
 */
open class BaseFragmentManagerHelper(
    protected val fragmentManager: FragmentManager,
    protected val containerViewId: Int,
    protected val fragments: List<Fragment>
) {
    protected val TAG = this::class.simpleName!!

    protected var currentIndex = 0

    init {
        initializeFragments()
    }

    /**
     * 添加所有的fragment并隐藏(除了首页)
     */
    protected fun initializeFragments() {
        // made by ai which is crazy
        fragmentManager.beginTransaction().apply {
            setReorderingAllowed(true)
            fragments.forEachIndexed { index, fragment ->
                add(containerViewId, fragment, fragment.javaClass.name)
                if (index != 0) hide(fragment)
            }
        }.commitNow()
    }

    fun getCurrentFragment(): Fragment {
        return fragments[currentIndex]
    }

    /**
     * 切换至目标索引的fragment
     */
    fun switchToFragment(index: Int) {
        if (index < 0 || index >= fragments.size) {
            Log.e(TAG, "Invalid index: $index")
            return
        }

        if (index == currentIndex) return

        fragmentManager.beginTransaction().apply {
            fragments[currentIndex].apply {
                hide(this)
            }
            fragments[index].apply {
                show(this)
            }
        }.commitNow()

        currentIndex = index
    }
}
