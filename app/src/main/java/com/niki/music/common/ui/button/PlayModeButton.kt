package com.niki.music.common.ui.button

import android.content.Context
import android.util.AttributeSet
import com.niki.music.MusicService
import com.niki.music.R

class PlayModeButton(context: Context, attr: AttributeSet?) : RippleButton(context, attr) {
    companion object {
        const val LOOP = MusicService.LOOP
        const val SINGLE = MusicService.SINGLE
        const val RANDOM = MusicService.RANDOM
    }

    private val map = linkedMapOf(
        LOOP to R.drawable.ic_loop,
        SINGLE to R.drawable.ic_single,
        RANDOM to R.drawable.ic_random
    )

    init {
        setImageResources(map)
    }
}