package com.niki.music.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import java.util.Stack

open class Host1(
    private val viewId: Int,
) : Stack<Pair<Int, Class<out Fragment>>>() {

    lateinit var fragmentManager: FragmentManager

    private fun witFragment(tag: Int, callback: (Fragment) -> Unit) {
        val f = findFragment(tag)
        f?.let { callback(it) }
    }

    private fun findFragment(tag: Int): Fragment? {
        if (fragmentManager.isDestroyed) return null
        return fragmentManager.findFragmentByTag(tag.toString())
    }

    // 用于切换不同的 host
    fun showTransaction(transaction: FragmentTransaction): FragmentTransaction {
        transaction.apply {
            peek()?.let {
                witFragment(it.first) { fragment ->
                    show(fragment)
                }
            }
            return this
        }
    }

    // 用于切换不同的 host
    fun hideTransaction(transaction: FragmentTransaction): FragmentTransaction {
        transaction.apply {
            peek()?.let {
                witFragment(it.first) { fragment ->
                    hide(fragment)
                }
            }
            return this
        }
    }

    private fun popOnceTransaction(transaction: FragmentTransaction): FragmentTransaction {
        transaction.apply {
            pop()?.run {
                witFragment(first) { fragment ->
                    hide(fragment)
                    remove(fragment)
                }
            }
            return this
        }
    }

    /**
     * @param enter 进入动画
     */
    fun show(enter: Int = 0) {
        if (fragmentManager.isDestroyed) return
        fragmentManager.beginTransaction().apply {
            setCustomAnimations(enter, 0)
            showTransaction(this)
        }.commit()
        fragmentManager.executePendingTransactions()
    }

    /**
     * @param exit 退出动画
     */
    fun hide(exit: Int = 0) {
        if (fragmentManager.isDestroyed) return
        fragmentManager.beginTransaction().apply {
            setCustomAnimations(0, exit)
            hideTransaction(this)
        }.commit()
        fragmentManager.executePendingTransactions()
    }

    fun popFragment(enter: Int = 0, exit: Int = 0): Boolean {
        if (!empty() && !fragmentManager.isDestroyed) {
            fragmentManager.beginTransaction().apply {
                setCustomAnimations(enter, exit)
                popOnceTransaction(this)
                showTransaction(this)
            }.commit()
            fragmentManager.executePendingTransactions()
            return true
        }
        return false
    }

    fun pushFragment(tag: Int, fragmentClazz: Class<out Fragment>, enter: Int = 0, exit: Int = 0) {
        createFragmentInstance(fragmentClazz)?.let { pushFragment(tag, it, enter, exit) }
    }

    fun pushFragment(tag: Int, fragment: Fragment, enter: Int = 0, exit: Int = 0) {
        if (fragmentManager.isDestroyed) return
        fragmentManager.beginTransaction().apply {
            setCustomAnimations(enter, exit)
            getPeekFragment()?.let { hide(it) }
            push(Pair(tag, fragment::class.java))
            add(viewId, fragment, tag.toString())
        }.commit()
        fragmentManager.executePendingTransactions()
    }

    fun navigateFragment(tag: Int, enter: Int = 0, exit: Int = 0): Boolean {
        if (!isPushed(tag) || fragmentManager.isDestroyed) return false
        fragmentManager.beginTransaction().apply {
            setCustomAnimations(enter, exit)
            while (tag != peek().first) {
                popOnceTransaction(this)
            }
            showTransaction(this)
        }.commit()
        fragmentManager.executePendingTransactions()
        return true
    }

    fun recreateFragments(newManager: FragmentManager = fragmentManager) {
        fragmentManager = newManager
        newManager.beginTransaction().apply {
            forEach { pair ->
                createFragmentInstance(pair.second)?.let {
                    add(viewId, it, pair.first.toString())
                    hide(it)
                }
            }
        }.commit()
        newManager.executePendingTransactions()
    }

    private fun isPushed(tag: Int) = searchByTag(tag) != -1

    private fun searchByTag(tag: Int): Int {
        if (empty()) return -1
        forEachIndexed { index, pair ->
            if (pair.first == tag) return index
        }
        return -1
    }

    fun getPeekPair(): Pair<Int, Class<out Fragment>>? {
        if (empty()) return null
        return peek()
    }

    private fun getPeekFragment(): Fragment? {
        if (empty()) return null
        return findFragment(peek().first)
    }

    /**
     * 尝试用无参的构造函数实例化并返回一个 fragment
     *
     * 没有无参的构造函数会崩溃, 但是我不懂为什么
     */
    private fun createFragmentInstance(clazz: Class<out Fragment>): Fragment? {
        val f: Fragment?
        try {
            f = clazz.getDeclaredConstructor().newInstance()
            return f
        } catch (_: Exception) {
            return null
        }
    }
}