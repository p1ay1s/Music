package com.niki.music.listen

import android.os.Build
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.niki.music.R
import com.niki.music.common.MusicRepository
import com.niki.music.common.commonViewModels.MusicViewModel
import com.niki.music.databinding.FragmentListenBinding
import com.niki.music.listen.ui.HotPlaylistAdapter
import com.niki.music.listen.ui.TopPlaylistAdapter
import com.niki.utils.base.BaseFragment
import com.niki.utils.base.logE
import com.niki.utils.base.ui.BaseLayoutManager
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class ListenFragment : BaseFragment<FragmentListenBinding>() {
    private lateinit var topAdapter: TopPlaylistAdapter
    private lateinit var topLayoutManager: BaseLayoutManager

    private lateinit var hotAdapter: HotPlaylistAdapter
    private lateinit var hotLayoutManager: BaseLayoutManager

    private val musicViewModel: MusicViewModel by activityViewModels<MusicViewModel>()
    private val listenViewModel: ListenViewModel by viewModels()

    private var mIsLoading = false

    private lateinit var itemAnimation: Animation

    override fun FragmentListenBinding.initBinding() {
        initValues()
        handleStates()

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
                                listenViewModel.sendIntent(ListenIntent.GetTopPlaylists())
                            }
                        }
                    }
                }
            })
        }

        recyclerViewHot.apply {
            PagerSnapHelper().attachToRecyclerView(this)
            adapter = hotAdapter
            layoutManager = hotLayoutManager
            animation = itemAnimation
        }
    }

    override fun onResume() {
        super.onResume()
        MusicRepository.run {
            if (mTopPlaylists.isEmpty())
                listenViewModel.sendIntent(ListenIntent.GetTopPlaylists())

            if (mHotPlaylists.isEmpty())
                listenViewModel.sendIntent(ListenIntent.GetHotPlaylists)
        }
    }

    private fun initValues() {
        topAdapter = TopPlaylistAdapter { }
        topLayoutManager = BaseLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, 6)

        hotAdapter = HotPlaylistAdapter(musicViewModel) {}
        hotLayoutManager = BaseLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, 6)

        itemAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in_anim)
    }

    private fun handleStates() = lifecycleScope.launch {
        listenViewModel.uiEffectFlow
            .collect {
                logE(TAG, "collected ${it::class.qualifiedName.toString()}")
                when (it) {
                    is ListenEffect.GetTopPlaylistsEffect -> {
                        if (it.isSuccess)
                            topAdapter.submitList(MusicRepository.mTopPlaylists)
                        mIsLoading = false
                    }

                    is ListenEffect.GetHotPlaylistsEffect -> {
                        if (it.isSuccess)
                            hotAdapter.submitList(MusicRepository.mHotPlaylists)
                    }
                }
            }
    }
}