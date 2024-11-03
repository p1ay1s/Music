package com.niki.music.listen.top

import android.os.Build
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.niki.common.repository.dataclasses.playlist.Playlist
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.utils.setViewBelowStatusBar
import com.niki.music.MainActivity
import com.niki.music.appFadeInAnim
import com.niki.music.databinding.FragmentTopPlaylistBinding
import com.niki.music.listen.top.TopPlaylistAdapter.Companion.PLAYLIST_SONGS_LIMIT
import com.niki.music.ui.SongAdapter
import com.niki.music.ui.SongAdapterListener
import com.niki.music.ui.showSongDetail
import com.niki.music.viewModel.MainViewModel
import com.p1ay1s.base.extension.addLineDecoration
import com.p1ay1s.base.extension.addOnLoadMoreListener_V
import com.p1ay1s.base.extension.loadImage
import com.p1ay1s.base.ui.PreloadLayoutManager
import com.p1ay1s.impl.ViewBindingFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlaylistFragment : ViewBindingFragment<FragmentTopPlaylistBinding>() {

    private val mainViewModel: MainViewModel by activityViewModels<MainViewModel>()

    private lateinit var songAdapter: SongAdapter
    private lateinit var baseLayoutManager: PreloadLayoutManager

    private var page = 1
    private var hasMore = true
    private var isLoading = false

    private lateinit var playlist: Playlist
    private lateinit var songs: List<Song>

    private fun getPair() {
        mainViewModel.run {
            playlistMap[this@PlaylistFragment.tag].let { pair ->
                playlist = pair!!.first
                songs = pair.second
            }
        }
    }

    override fun FragmentTopPlaylistBinding.initBinding() {
        getPair()
        initValues()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            setViewBelowStatusBar(toolbar)

        appbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val alpha = (-verticalOffset / appBarLayout.totalScrollRange.toFloat())
            block.alpha = 1 - alpha * alpha
        }

        toolbar.title = playlist.name
        description.text = playlist.description
        if (playlist.description.isBlank()) {
            q.visibility = View.INVISIBLE
        }

        background.loadImage(playlist.coverImgUrl)

        with(binding.recyclerView) {
            adapter = songAdapter
            layoutManager = baseLayoutManager
            animation = appFadeInAnim

            addLineDecoration(requireActivity(), LinearLayout.VERTICAL)

            addOnLoadMoreListener_V(1) { loadMore() }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(250) // 避免 recyclerview 的更新打断 fragment 的启动动画
            songAdapter.submitList(songs)
        }
    }

    private fun loadMore() {
        if (isLoading || !hasMore || playlist.id == "") return
        isLoading = true
        mainViewModel.getSongsFromPlaylist(
            playlist.id,
            PLAYLIST_SONGS_LIMIT,
            page * PLAYLIST_SONGS_LIMIT
        ) { newSongs ->
            if (newSongs != null) {
                page++
                hasMore = newSongs.isNotEmpty()
                songs += newSongs
                songAdapter.submitList(this.songs)
            }
            isLoading = false
        }
    }

    private fun initValues() {
        songAdapter = SongAdapter(
            showDetails = true,
            showImage = false
        )
        songAdapter.setSongAdapterListener(SongAdapterListenerImpl())
        baseLayoutManager = PreloadLayoutManager(
            requireActivity(),
            LinearLayoutManager.VERTICAL,
            4
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        songAdapter.setSongAdapterListener(null)
    }

    inner class SongAdapterListenerImpl : SongAdapterListener {
        override fun onPlayMusic(list: List<Song>) {
            (activity as MainActivity).onSongPass(list)
        }

        override fun onMoreClicked(song: Song) {
            showSongDetail(song)
        }
    }
}