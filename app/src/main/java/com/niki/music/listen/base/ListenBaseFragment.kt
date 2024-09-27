package com.niki.music.listen.base

import android.os.Build
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.niki.music.R
import com.niki.music.common.MusicRepository
import com.niki.music.common.views.IView
import com.niki.music.databinding.FragmentListenBinding
import com.niki.music.listen.ui.TopPlaylistAdapter
import com.p1ay1s.dev.base.TAG
import com.p1ay1s.dev.log.logE
import com.p1ay1s.dev.ui.PreloadLayoutManager
import com.p1ay1s.dev.viewbinding.ViewBindingFragment
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class ListenBaseFragment : ViewBindingFragment<FragmentListenBinding>(), IView {
    private lateinit var topAdapter: TopPlaylistAdapter
    private lateinit var topLayoutManager: PreloadLayoutManager

    private val listenBaseViewModel: ListenBaseViewModel by viewModels()

    private var mIsLoading = false

    private lateinit var itemAnimation: Animation

    override fun FragmentListenBinding.initBinding() {
        initValues()
        handle()

        recyclerViewTop.apply {
            PagerSnapHelper().attachToRecyclerView(this)
            adapter = topAdapter
            layoutManager = topLayoutManager
            animation = itemAnimation

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    recyclerView.apply {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            if (!canScrollHorizontally(0) && !mIsLoading) {
                                mIsLoading = true
                                listenBaseViewModel.sendIntent(ListenIntent.GetTopPlaylists())
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
            if (mTopPlaylists.isEmpty())
                listenBaseViewModel.sendIntent(ListenIntent.GetTopPlaylists())
        }
    }

    private fun initValues() {
        topAdapter = TopPlaylistAdapter { }
        topLayoutManager = PreloadLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            6
        )

        itemAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in_anim)
    }

    override fun handle() = listenBaseViewModel.run {
        lifecycleScope.launch {
            uiEffectFlow
                .collect {
                    logE(TAG, "COLLECTED ${it::class.qualifiedName.toString()}")
                    when (it) {
                        is ListenEffect.GetTopPlaylistsEffect -> {
                            if (it.isSuccess)
                                topAdapter.submitList(MusicRepository.mTopPlaylists)
                            mIsLoading = false
                        }
                    }
                }
        }
    }
}