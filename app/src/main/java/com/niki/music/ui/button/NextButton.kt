package com.niki.music.ui.button

import android.content.Context
import android.util.AttributeSet
import com.niki.music.R

class NextButton(context: Context, attr: AttributeSet?) : RippleButton(context, attr) {
    init {
        setImageResource(R.drawable.ic_next)
    }
}