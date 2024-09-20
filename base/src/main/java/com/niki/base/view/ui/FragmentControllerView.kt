package com.niki.base.view.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.niki.base.log.getFunctionName
import com.niki.base.log.logE



/**
 * 功能为简单的fragment切换
 */
open class FragmentManagerHelper(
    protected val fragmentManager: FragmentManager,
    protected val containerViewId: Int,
    protected val fragmentMap: LinkedHashMap<String, Fragment>
) {
    protected val TAG = this::class.simpleName!!

    protected var currentIndex = fragmentMap.keys.first()

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
            fragmentMap.forEach { (index, fragment) ->
                add(containerViewId, fragment, index)
                hide(fragment)
            }

            fragmentMap[currentIndex]?.let { show(it) }
        }.commit()
    }

    fun getCurrentFragment() = getFragment(currentIndex)

    private fun getFragment(index: String): Fragment {
        val fragment = fragmentMap[index]
            ?: throw RuntimeException("${getFunctionName()}cannot find fragment with index $index")
        return fragment
    }

    /**
     * 切换至目标索引的fragment
     */
    fun switchToFragment(index: String) {
        fragmentMap.keys.forEach {
            if (it == index) {
                if (index == currentIndex) return
                fragmentManager.beginTransaction().apply {
                    hide(getCurrentFragment())
                    show(getFragment(index))
                }.commitNow()

                currentIndex = index
                return
            }
        }
        throw RuntimeException("${getFunctionName()}cannot find fragment with index $index")
    }

    /**
     * 添加并显示
     */
    fun addAndSwitchToFragment(index: String, fragment: Fragment) {
        fragmentMap.keys.forEach {
            if (it == index) {
                logE(TAG, "index $index is already added!")
                return
            }
        }
        fragmentManager.beginTransaction().apply {
            fragmentMap[index] = fragment
            add(containerViewId, fragment, index)
            switchToFragment(index)
        }.commitNow()
    }

    /**
     * 移除
     */
    fun deleteFragment(deleteIndex: String) {
        fragmentManager.beginTransaction().apply {
            getFragment(deleteIndex).let {
                fragmentMap.remove(deleteIndex)
                hide(it)
                detach(it)
            }
        }.commitNow()
    }
}
