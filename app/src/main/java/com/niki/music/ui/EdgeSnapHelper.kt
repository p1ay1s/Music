package com.niki.music.ui

import android.view.View
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.SnapHelper
import kotlin.math.abs

fun RecyclerView.setEdgeSnapHelper() {
    if (onFlingListener == null)
        EdgeSnapHelper(RecyclerView.HORIZONTAL).attachToRecyclerView(this)
}

/**
 * 暂时实现左和上的吸附
 */
class EdgeSnapHelper(private val orientation: Int) : SnapHelper() {
    private var helper: OrientationHelper? = null

    companion object {
        const val HORIZONTAL = RecyclerView.HORIZONTAL
        const val VERTICAL = RecyclerView.VERTICAL
    }

    init {
        if (orientation != HORIZONTAL && orientation != VERTICAL)
            throw Exception("receive an invalid orientation!")
    }

    override fun calculateDistanceToFinalSnap(
        layoutManager: LayoutManager,
        targetView: View
    ): IntArray {
        val out = IntArray(2)
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val distance = if (orientation == HORIZONTAL) location[0] else location[1]

        when (orientation) {
            HORIZONTAL -> {
                if (layoutManager.canScrollHorizontally())
                    out[0] = distance - targetView.marginStart
                else out[0] = 0
            }

            VERTICAL -> {
                if (layoutManager.canScrollVertically())
                    out[1] = distance - targetView.marginTop
                else out[1] = 0
            }
        }

        return out
    }

    /**
     * 找要吸附的 view
     */
    override fun findSnapView(layoutManager: LayoutManager): View? {
        return findClosestToEdge(layoutManager)
    }

    /**
     * 根据速度判断滚动方向并返回目标位置的索引
     */
    override fun findTargetSnapPosition(
        layoutManager: LayoutManager,
        velocityX: Int,
        velocityY: Int
    ): Int {
        val itemCount = layoutManager.itemCount
        if (itemCount == 0) return RecyclerView.NO_POSITION

        val closestChild: View = findClosestToEdge(layoutManager) ?: return RecyclerView.NO_POSITION
        val position = layoutManager.getPosition(closestChild)

        return if (orientation == HORIZONTAL)
            if (velocityX > 0) position + 1 else position - 1
        else
            if (velocityY > 0) position + 1 else position - 1
    }

    /**
     * 遍历寻找最靠近左边缘的 view
     */
    private fun findClosestToEdge(layoutManager: LayoutManager): View? {
        var closestChild: View? = null // 初始化
        var minDistance = Int.MAX_VALUE
        val location = IntArray(2)

        val index = if (orientation == HORIZONTAL) 0 else 1

        // 遍历找到最靠近边缘的 view
        for (i in 0 until layoutManager.childCount) {
            val child = layoutManager.getChildAt(i) ?: continue
            child.getLocationOnScreen(location)
            val distance = abs(location[index]) // 数学差真的是吃亏....
            if (distance >= minDistance) continue

            closestChild = child
            minDistance = distance
        }
        return closestChild
    }

//    private fun getHelper(layoutManager: LayoutManager): OrientationHelper {
//        return if (helper == null || helper?.layoutManager != layoutManager) {
//
//            if (orientation == HORIZONTAL)
//                OrientationHelper.createHorizontalHelper(layoutManager).also { helper = it }
//            else
//                OrientationHelper.createVerticalHelper(layoutManager).also { helper = it }
//
//        } else {
//            helper!!
//        }
//    }
}