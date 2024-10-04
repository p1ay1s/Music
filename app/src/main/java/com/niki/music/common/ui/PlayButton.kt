package com.niki.music.common.ui

import android.content.Context
import com.niki.music.R
import com.p1ay1s.dev.ui.ChangingImageButton

class PlayButton(context: Context) : ChangingImageButton(context) {
    init {
        // TODO sources???????
        setImageResources(R.drawable.ic_play, R.drawable.ic_pause)
        setOnClickListener {
            Status.IMAGE_A
        }
    }
}