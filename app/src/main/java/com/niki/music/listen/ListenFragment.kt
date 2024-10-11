package com.niki.music.listen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.niki.common.repository.MusicRepository
import com.niki.common.values.FragmentTag
import com.niki.music.R
import com.niki.music.common.views.IView
import com.niki.music.databinding.FragmentListenBinding
import com.niki.music.listen.ui.TopPlaylistAdapter
import com.niki.music.listen.ui.TopPlaylistFragment
import com.p1ay1s.base.extension.TAG
import com.p1ay1s.base.extension.addOnLoadMoreListener_H
import com.p1ay1s.base.extension.findFragmentHost
import com.p1ay1s.base.extension.setSnapHelper
import com.p1ay1s.base.log.logE
import com.p1ay1s.base.ui.PreloadLayoutManager
import com.p1ay1s.impl.ViewBindingFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * TODO 需要保存的:
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class ListenFragment : ViewBindingFragment<FragmentListenBinding>(), IView {
    private lateinit var topAdapter: TopPlaylistAdapter
    private lateinit var topLayoutManager: PreloadLayoutManager

    private val listenViewModel: ListenViewModel by viewModels()

    private var mIsLoading = false
    private var mHandleJob: Job? = null

    override fun FragmentListenBinding.initBinding() {
        initValues()
        handle()

        with(binding.recyclerViewTop) {
            setSnapHelper()

            adapter = topAdapter
            layoutManager = topLayoutManager
            animation = com.niki.music.appFadeInAnim

            addOnLoadMoreListener_H(1) {
                topLoadMore()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        MusicRepository.run {
            when {
                topPlaylists.isEmpty() -> listenViewModel.sendIntent(ListenIntent.GetTopPlaylists())
                topAdapter.currentList.isEmpty() -> topAdapter.submitList(topPlaylists)
                else -> {}
            }
        }
    }

    private fun initValues() {
        topAdapter = TopPlaylistAdapter {
            findFragmentHost()?.add(
                FragmentTag.TOP_PLAYLIST_FRAGMENT,
                TopPlaylistFragment(),
                R.anim.right_enter,
                R.anim.fade_out
            )
        }
        topLayoutManager = PreloadLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            6
        )
    }

    override fun handle() = listenViewModel.run {
        mHandleJob?.cancel()
        mHandleJob = lifecycleScope.launch {
            uiEffectFlow
                .collect {
                    logE(TAG, "COLLECTED ${it::class.qualifiedName.toString()}")
                    when (it) {
                        is ListenEffect.GetTopPlaylistsEffect -> {
                            if (it.isSuccess)
                                topAdapter.submitList(MusicRepository.topPlaylists)
                            mIsLoading = false
                        }
                    }
                }
        }
    }

    private fun topLoadMore() {
        if (mIsLoading) return
        mIsLoading = true
        listenViewModel.sendIntent(ListenIntent.GetTopPlaylists())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mIsLoading = false
        mHandleJob?.cancel()
        mHandleJob = null
    }
}