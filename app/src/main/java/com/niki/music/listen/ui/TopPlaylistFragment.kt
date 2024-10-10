package com.niki.music.browse

import android.os.Build
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.niki.common.repository.dataclasses.Playlist
import com.niki.music.R
import com.niki.music.common.ui.SongAdapter
import com.niki.music.databinding.FragmentTopPlaylistBinding
import com.p1ay1s.dev.ui.PreloadLayoutManager
import com.p1ay1s.dev.util.ImageSetter.setImgView
import com.p1ay1s.dev.viewbinding.ViewBindingFragment
import kotlin.math.abs


var playlist: Playlist? = null

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class TopPlaylistFragment :
    ViewBindingFragment<FragmentTopPlaylistBinding>() {

    private lateinit var songAdapter: SongAdapter
    private lateinit var baseLayoutManager: PreloadLayoutManager
    private lateinit var itemAnimation: Animation

    override fun FragmentTopPlaylistBinding.initBinding() {
        initValues()

        appbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            toolbar.visibility =
                if (abs(verticalOffset) == appBarLayout.totalScrollRange) View.VISIBLE else View.GONE
        }

        playlist?.run {

            background.setImgView(coverImgUrl)
        }

        with(recyclerView) {
            adapter = songAdapter
            layoutManager = baseLayoutManager
            animation = itemAnimation

            if (itemDecorationCount != 0)
                addItemDecoration(
                    DividerItemDecoration(
                        requireActivity(),
                        androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
                    )
                )
        }
    }

    private fun initValues() {
        songAdapter = SongAdapter(
            enableCache = false,
            showDetails = true,
            showImage = false
        )
        baseLayoutManager = PreloadLayoutManager(
            requireActivity(),
            LinearLayoutManager.VERTICAL,
            4
        )
        itemAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in_anim)
    }
}