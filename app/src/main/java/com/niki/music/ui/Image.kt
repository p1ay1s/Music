package com.niki.music.ui

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

fun ImageView.loadCover(
    imgUrl: String,
    radius: Int,
    enableCrossFade: Boolean = true,
    preferences: RequestBuilder<Drawable>.() -> RequestBuilder<Drawable> = { this }
) {
    val drawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radius * 1.2F // 设置圆角半径
        setColor(Color.parseColor("#80000000")) // 设置颜色
    }
    this.visibility = View.VISIBLE
    Glide.with(this)
        .load(imgUrl)
        .fitCenter()
        .placeholder(drawable)
        .apply {
            if (enableCrossFade)
                transition(DrawableTransitionOptions.withCrossFade())
            transform(CenterCrop(), RoundedCorners(radius))
        }
        .preferences()
        .into(this)
}