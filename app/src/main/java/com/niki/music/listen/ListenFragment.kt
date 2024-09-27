package com.niki.music.listen

import android.os.Build
import androidx.annotation.RequiresApi
import com.niki.music.listen.base.ListenBaseFragment
import com.p1ay1s.extensions.views.ContainerFragment

const val LISTEN_BASE = "listen base"
const val LISTEN_PLAYLIST = "listen playlist"

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class ListenFragment : ContainerFragment(linkedMapOf(LISTEN_BASE to ListenBaseFragment()))