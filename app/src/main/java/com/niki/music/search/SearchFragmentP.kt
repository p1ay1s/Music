package com.niki.music.search

import android.os.Build
import androidx.annotation.RequiresApi
import com.niki.base.view.ContainerFragment

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class SearchFragmentP :
    ContainerFragment(linkedMapOf(PREVIEW to SearchFragment(), RESULT to SearchFragment())) {
    companion object {
        const val PREVIEW = "preview"
        const val RESULT = "result"
    }
}