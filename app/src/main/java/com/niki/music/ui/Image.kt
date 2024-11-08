package com.niki.music.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

fun getPlaceholder(radius: Int) = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    cornerRadius = radius * 1.2F // 设置圆角半径
    setColor(Color.parseColor("#80000000")) // 设置颜色
}

fun Context.loadDrawable(
    imgUrl: String,
    skipCache: Boolean = true,
    callback: ((Drawable) -> Unit)? = null
) {
    Glide.with(this)
        .load(imgUrl)
        .skipMemoryCache(skipCache)
        .placeholder(getPlaceholder(0))
        .into(object : CustomTarget<Drawable>() {
            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable>?
            ) {
                callback?.invoke(resource)
            }

            override fun onLoadCleared(placeholder: Drawable?) {}
        })
}

fun ImageView.loadCCover(
    drawable: Drawable,
    radius: Int,
    enableCrossFade: Boolean = true
) {
    this.visibility = View.VISIBLE
    Glide.with(this)
        .load(drawable)
        .placeholder(getPlaceholder(radius))
        .apply {
            if (enableCrossFade)
                transition(DrawableTransitionOptions.withCrossFade())
            transform(CenterCrop(), RoundedCorners(radius))
        }
        .into(this)
}

fun Context.loadBlurDrawable(
    drawable: Drawable,
    value: Int,
    callback: ((Drawable) -> Unit)? = null
) {
    // 直接对 drawable 进行 override 不可行, 必须是网络 url 才能正确缩小
    // 先将 Drawable 转换为 Bitmap 然后缩小 Bitmap
    val bitmap = (drawable as BitmapDrawable).bitmap
    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, true)
    Glide.with(this)
        .load(scaledBitmap)
        .fitCenter()
        .transform(BlurTransformation(this, value))
        .into(object : CustomTarget<Drawable?>() {
            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable?>?
            ) {
                callback?.invoke(resource)
            }

            override fun onLoadCleared(placeholder: Drawable?) {}
        })
}