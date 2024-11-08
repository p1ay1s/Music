package com.niki.music.ui

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatSeekBar
import kotlin.math.roundToInt

/**
 * 支持显示加载状态的 seekbar
 */
class LoadingSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatSeekBar(context, attrs, defStyleAttr) {
    private var isLoading = false
    private var loadingAnimator: ValueAnimator? = null

    private val animDuration = 600L        // 时长
    private val startPos = 0f             // 起始进度
    private val endPos = 1f               // 结束进度
    private val customInterpolator = LinearInterpolator() // 匀速插值器

    init {
        loadingAnimator = ValueAnimator.ofFloat(startPos, endPos).apply {
            duration = animDuration
            repeatCount = ValueAnimator.INFINITE // 重复次数
            repeatMode = ValueAnimator.REVERSE // 重复模式(REVERSE反向)
            interpolator = customInterpolator // 插值器

            addUpdateListener { animation ->
                // 实现一个右出左进的加载条动画
                if (animation.currentPlayTime % animDuration < 50L) // 约为 duration 的倍数
                    rotation =
                        if (animation.currentPlayTime % (animDuration * 2) < 50L) 0F else 180F // 0, 2 * duration, 4 * duration... 设置旋转为 0, 否则 180

                val progress = animation.animatedValue as Float
                setProgress((progress * max).roundToInt())
            }
        }
    }

    fun setLoading(loading: Boolean) {
        if (this.isLoading == loading) return

        this.isLoading = loading
        isEnabled = !isLoading

        if (loading) {
            loadingAnimator?.start()
        } else {
            loadingAnimator?.cancel()
            rotation = 0F
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isLoading) {
            return false // 加载状态下直接消费掉
        }
        return super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (loadingAnimator != null) {
            loadingAnimator!!.cancel()
        }
    }
}