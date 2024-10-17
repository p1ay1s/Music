package com.niki.music.common.ui.button

import android.content.Context
import android.util.AttributeSet
import com.niki.music.R

class PreviousButton(context: Context, attr: AttributeSet?) : RippleButton(context, attr) {
    init {
        setImageResource(R.drawable.ic_previous)
    }
}