package com.niki.music.common.ui.button

import android.content.Context
import android.util.AttributeSet
import com.niki.music.R

class PlayButton(context: Context, attr: AttributeSet?) : RippleButton(context, attr) {
    companion object {
        const val PLAY = "play"
        const val PAUSE = "pause"
    }

   private val map = linkedMapOf(
        PLAY to R.drawable.ic_play,
        PAUSE to R.drawable.ic_pause
    )

    init {
        setImageResources(map)
    }
}