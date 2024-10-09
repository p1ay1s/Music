package com.niki.music.listen

import android.os.Build
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.niki.common.repository.MusicRepository
import com.niki.common.values.FragmentTag
import com.niki.music.R
import com.niki.music.browse.TopPlaylistFragment
import com.niki.music.common.views.IView
import com.niki.music.databinding.FragmentListenBinding
import com.niki.music.listen.ui.TopPlaylistAdapter
import com.niki.music.listenIndex
import com.p1ay1s.dev.base.TAG
import com.p1ay1s.dev.base.findFragmentHost
import com.p1ay1s.dev.log.logE
import com.p1ay1s.dev.ui.PreloadLayoutManager
import com.p1ay1s.dev.viewbinding.ViewBindingFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class ListenFragment : ViewBindingFragment<FragmentListenBinding>(), IView {
    private lateinit var topAdapter: TopPlaylistAdapter
    private lateinit var topLayoutManager: PreloadLayoutManager
    private lateinit var itemAnimation: Animation

    private val listenViewModel: ListenViewModel by viewModels()

    private var mIsLoading = false
    private var mHandleJob: Job? = null

    override fun FragmentListenBinding.initBinding() {
        initValues()
        handle()

        with(recyclerViewTop) {
            PagerSnapHelper().attachToRecyclerView(this)

            adapter = topAdapter
            layoutManager = topLayoutManager
            animation = itemAnimation

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    recyclerView.apply {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            if (!canScrollHorizontally(-1) && !mIsLoading) {
                                mIsLoading = true
                                listenViewModel.sendIntent(ListenIntent.GetTopPlaylists())
                            }
                        }
                    }
                }
            })
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
            listenIndex = FragmentTag.TOP_PLAYLIST_FRAGMENT
            findFragmentHost()?.add(FragmentTag.TOP_PLAYLIST_FRAGMENT, TopPlaylistFragment())
        }
        topLayoutManager = PreloadLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            6
        )
        itemAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in_anim)
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

    override fun onDestroyView() {
        super.onDestroyView()
        mIsLoading = false
        mHandleJob?.cancel()
        mHandleJob = null
    }
}