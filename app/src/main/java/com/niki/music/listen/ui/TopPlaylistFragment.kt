package com.niki.music.listen.ui

import android.widget.LinearLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.niki.common.repository.dataclasses.song.Song
import com.niki.music.appFadeInAnim
import com.niki.music.common.ui.SongAdapter
import com.niki.music.common.ui.SongAdapterListener
import com.niki.music.common.viewModels.MainViewModel
import com.niki.music.databinding.FragmentTopPlaylistBinding
import com.niki.music.intents.MainIntent
import com.p1ay1s.base.extension.addLineDecoration
import com.p1ay1s.base.extension.addOnLoadMoreListener_V
import com.p1ay1s.base.extension.toast
import com.p1ay1s.base.ui.PreloadLayoutManager
import com.p1ay1s.impl.ViewBindingFragment
import com.p1ay1s.util.ImageSetter.setImgView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class TopPlaylistFragment :
    ViewBindingFragment<FragmentTopPlaylistBinding>(), SongAdapterListener {

    private val mainViewModel: MainViewModel by activityViewModels<MainViewModel>()

    private lateinit var songAdapter: SongAdapter
    private lateinit var baseLayoutManager: PreloadLayoutManager

    override fun FragmentTopPlaylistBinding.initBinding() {
        initValues()

        appbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val alpha = (-verticalOffset / appBarLayout.totalScrollRange.toFloat())
            background.alpha = 1 - alpha * alpha
//            toolbar.visibility =
//                if (abs(verticalOffset) == appBarLayout.totalScrollRange) View.VISIBLE else View.INVISIBLE
        }

        toolbar.title = currentTopPlaylist!!.name

        currentTopPlaylist?.run {
            background.setImgView(coverImgUrl)
        }

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
            delay(250)
            songAdapter.submitList(currentTopSongs)
        }
    }

    private fun loadMore() {
        if (isTopLoading || !currentTopHasMore) return
        isTopLoading = true
        mainViewModel.getSongsFromPlaylist(
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
        }
    }

    private fun initValues() {
        songAdapter = SongAdapter(
            enableCache = false,
            showDetails = true,
            showImage = false
        )
        songAdapter.setSongAdapterListener(this)
        baseLayoutManager = PreloadLayoutManager(
            requireActivity(),
            LinearLayoutManager.VERTICAL,
            4
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        songAdapter.removeSongAdapterListener()
    }

    override fun onPlayMusic(song: Song) {
        mainViewModel.sendIntent(MainIntent.TryPlaySong(song))
    }

    override fun onMoreClicked(song: Song) {
        toast("more -> ${song.name}")
    }
}