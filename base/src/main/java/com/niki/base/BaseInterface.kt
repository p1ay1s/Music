package com.niki.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import java.lang.reflect.ParameterizedType

interface BaseInterface<VB : ViewDataBinding> {
    val TAG
        get() = this::class.simpleName!!

    /**
     * 查找类名中有"binding"的索引, 不保证绝对正确
     */
    fun getPosition(vbClass: List<Class<VB>>): Int {
        // 找到包含 "binding" 的索引
        val position = vbClass.indexOfFirst { it.name.contains("binding", ignoreCase = true) }

        // if "binding" not found, use 0
        return if (position != -1) position else 0
    }

    @Suppress("UNCHECKED_CAST")
    fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): VB = try {
        with((javaClass.genericSuperclass as ParameterizedType).actualTypeArguments.filterIsInstance<Class<VB>>()) {
            val inflateMethod = this[getPosition(this)].getDeclaredMethod(
                "inflate",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Boolean::class.java
            )
            val dataBinding = inflateMethod.invoke(null, inflater, container, false) as VB
            return dataBinding
        }
    } catch (e: Exception) {
        e.printStackTrace()
        throw IllegalArgumentException("can not get ViewBinding instance through reflection!")
    }

    @Suppress("UNCHECKED_CAST")
    fun getViewBinding(inflater: LayoutInflater): VB = try {
        with((javaClass.genericSuperclass as ParameterizedType).actualTypeArguments.filterIsInstance<Class<VB>>()) {
            val inflateMethod =
                this[getPosition(this)].getDeclaredMethod("inflate", LayoutInflater::class.java)
            val dataBinding = inflateMethod.invoke(null, inflater) as VB
            return dataBinding
        }
    } catch (e: Exception) {
        e.printStackTrace()
        throw IllegalArgumentException("can not get ViewBinding instance through reflection!")
    }
}