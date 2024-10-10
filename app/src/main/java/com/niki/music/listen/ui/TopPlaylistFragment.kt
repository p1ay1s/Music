package com.niki.music.listen.ui

import android.os.Build
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.niki.common.utils.addLineDecoration
import com.niki.common.utils.addOnLoadMoreListener_V
import com.niki.music.appFadeInAnim
import com.niki.music.appMainViewModel
import com.niki.music.common.ui.SongAdapter
import com.niki.music.databinding.FragmentTopPlaylistBinding
import com.p1ay1s.dev.ui.PreloadLayoutManager
import com.p1ay1s.dev.util.ImageSetter.setImgView
import com.p1ay1s.dev.viewbinding.ViewBindingFragment
import kotlin.math.abs


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class TopPlaylistFragment :
    ViewBindingFragment<FragmentTopPlaylistBinding>() {

    private lateinit var songAdapter: SongAdapter
    private lateinit var baseLayoutManager: PreloadLayoutManager

    override fun FragmentTopPlaylistBinding.initBinding() {
        initValues()

        appbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            toolbar.visibility =
                if (abs(verticalOffset) == appBarLayout.totalScrollRange) View.VISIBLE else View.GONE
        }

        toolbar.title = currentTopPlaylist!!.name

        currentTopPlaylist?.run {
            background.setImgView(coverImgUrl, enableCrossFade = false)
        }

        songAdapter.submitList(currentTopSongs)
    }

    override fun onResume() {
        super.onResume()

        with(binding.recyclerView) {
            adapter = songAdapter
            layoutManager = baseLayoutManager
            animation = appFadeInAnim

            addLineDecoration(requireActivity(), LinearLayout.VERTICAL)

            addOnLoadMoreListener_V(1) { loadMore() }
        }
    }

    private fun loadMore() {
        if (isTopLoading || !currentTopHasMore) return
        isTopLoading = true
//        binding.tail.visibility = View.VISIBLE
//        binding.tail.baselineAlignBottom = true
        appMainViewModel.getSongsFromPlaylist(
            currentTopPlaylist!!.id,
            PLAYLIST_SONGS_LIMIT,
            currentTopPage * PLAYLIST_SONGS_LIMIT
        ) { songs ->
            if (songs != null) {
                currentTopPage++
                currentTopHasMore = songs.isNotEmpty()
                if (currentTopSongs == null) {
                    currentTopSongs = songs
                } else {
                    currentTopSongs = currentTopSongs!!.plus(songs)
                }
                songAdapter.submitList(currentTopSongs)
            }
            isTopLoading = false
//            binding.tail.visibility = View.GONE
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
    }
}