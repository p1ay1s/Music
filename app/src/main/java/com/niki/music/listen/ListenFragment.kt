package com.niki.music.listen


import android.content.Context
import android.os.Build
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.niki.common.repository.dataclasses.playlist.Playlist
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.utils.getLargeRandomNum
import com.niki.common.utils.setViewBelowStatusBar
import com.niki.music.R
import com.niki.music.databinding.FragmentListenBinding
import com.niki.music.listen.ListenViewModel.Companion.TOP_LIMIT
import com.niki.music.listen.top.PlaylistFragment
import com.niki.music.listen.top.TopPlaylistAdapter
import com.niki.music.listen.top.TopPlaylistAdapterListener
import com.niki.music.viewModel.MainViewModel
import com.p1ay1s.base.extension.addOnLoadMoreListener_H
import com.p1ay1s.base.extension.findHost
import com.p1ay1s.base.extension.setSnapHelper
import com.p1ay1s.base.ui.PreloadLayoutManager
import com.p1ay1s.vbclass.ViewBindingFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ListenFragment : ViewBindingFragment<FragmentListenBinding>() {
    private lateinit var topAdapter: TopPlaylistAdapter
    private lateinit var topLayoutManager: ToMiddleLayoutManager

    private lateinit var listenViewModel: ListenViewModel
    private val mainViewModel: MainViewModel by activityViewModels<MainViewModel>()

    private var playlistJob: Job? = null

    override fun FragmentListenBinding.initBinding() {
        listenViewModel = ViewModelProvider(requireActivity())[ListenViewModel::class.java]

        initValues()
        handle()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            setViewBelowStatusBar(recyclerViewTop)

        with(binding.recyclerViewTop) {
            setSnapHelper()

            adapter = topAdapter
            layoutManager = topLayoutManager
            animation = com.niki.music.appFadeInAnim

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) // 已停止
                        listenViewModel.position = getItemRealPosition()// 第一个可见的是左边露出部分的
                }
            })

            addOnLoadMoreListener_H(1) {
                listenViewModel.sendIntent(ListenIntent.GetTopPlaylists())
            }
        }
    }

    override fun onResume() {
        super.onResume()

        listenViewModel.state.apply {
            if (playlists == null)
                listenViewModel.sendIntent(ListenIntent.GetTopPlaylists(true))
            else {
                tryScrollBack()
            }
        }
    }

    private fun getItemRealPosition(): Int {
        return topLayoutManager.findFirstCompletelyVisibleItemPosition()
    }

    private fun tryScrollBack() = runCatching {
        topLayoutManager.scrollToPosition(listenViewModel.position)
        requireScroll()
    }

    private fun requireScroll(position: Int = listenViewModel.position) {
        if (getItemRealPosition() != position)
            binding.recyclerViewTop.smoothScrollToPosition(position)
    }

    private fun initValues() {
        topAdapter = TopPlaylistAdapter()
        topAdapter.setOnTopPlaylistAdapterListener(TopPlaylistAdapterListenerImpl())

        topLayoutManager = ToMiddleLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            TOP_LIMIT
        )
    }

    private fun handle() = listenViewModel.run {
        playlistJob?.cancel()

        observeState {
            playlistJob = lifecycleScope.launch {
                map { it.playlists }.distinctUntilChanged().filterNotNull().collect {
                    topAdapter.submitList(it)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        topAdapter.setOnTopPlaylistAdapterListener(null)
        playlistJob?.cancel()
        playlistJob = null
    }

    inner class TopPlaylistAdapterListenerImpl : TopPlaylistAdapterListener {
        override fun onContact(position: Int): Boolean {
            if (getItemRealPosition() == position) { // 是居中的 item , 打开
                return true
            } else {
                requireScroll(position) // 不是居中的 , 滚动使其居中
                return false
            }
        }

        // adapter 准备完成 , 添加 fragment
        override fun onReady(playlist: Playlist, songs: List<Song>) {
            val num = getLargeRandomNum()
            mainViewModel.playlistMap[num.toString()] = Pair(playlist, songs)
            findHost()?.pushFragment(
                num,
                PlaylistFragment(),
                R.anim.right_enter,
                R.anim.fade_out
            )
        }
    }

    /**
     * 具有预加载功能的 layoutManager 子类
     */
    class ToMiddleLayoutManager(
        context: Context,
        orientation: Int,
        size: Int = 4,
        reverseLayout: Boolean = false
    ) : PreloadLayoutManager(context, orientation, size, reverseLayout) {
        override fun smoothScrollToPosition(
            recyclerView: RecyclerView,
            state: RecyclerView.State,
            position: Int
        ) {
            val smoothScroller = CenterSmoothScroller(recyclerView.context)
            smoothScroller.targetPosition = position
            startSmoothScroll(smoothScroller)
        }

        private class CenterSmoothScroller(context: Context) : LinearSmoothScroller(context) {
            override fun calculateDtToFit(
                viewStart: Int,
                viewEnd: Int,
                boxStart: Int,
                boxEnd: Int,
                snapPreference: Int
            ): Int {
                return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
            }
        }
    }
}