package com.niki.music.ui.button

import android.content.Context
import android.util.AttributeSet
import com.niki.music.MusicService
import com.niki.music.R

class PlayModeButton(context: Context, attr: AttributeSet?) : RippleButton(context, attr) {

    private val map = linkedMapOf(
        MusicService.LOOP to R.drawable.ic_loop,
        MusicService.SINGLE to R.drawable.ic_single,
        MusicService.RANDOM to R.drawable.ic_random
    )

    init {
        setImageResources(map)
    }
}