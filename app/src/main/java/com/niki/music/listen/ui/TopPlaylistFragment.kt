package com.niki.music.listen.ui

import android.os.Build
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.niki.music.appFadeInAnim
import com.niki.music.common.ui.SongAdapter
import com.niki.music.common.viewModels.MainViewModel
import com.niki.music.databinding.FragmentTopPlaylistBinding
import com.p1ay1s.base.extension.addLineDecoration
import com.p1ay1s.base.extension.addOnLoadMoreListener_V
import com.p1ay1s.base.ui.PreloadLayoutManager
import com.p1ay1s.impl.ViewBindingFragment
import com.p1ay1s.util.ImageSetter.setImgView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class TopPlaylistFragment :
    ViewBindingFragment<FragmentTopPlaylistBinding>() {

    private val mainViewModel: MainViewModel by activityViewModels<MainViewModel>()

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
        lifecycleScope.launch() {
            delay(250)
            songAdapter.submitList(currentTopSongs)
        }
    }

    private fun loadMore() {
        if (isTopLoading || !currentTopHasMore) return
        isTopLoading = true
//        binding.tail.visibility = View.VISIBLE
//        binding.tail.baselineAlignBottom = true
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