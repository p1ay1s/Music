package com.niki.music.search.result

import android.os.Build
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SearchView
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.niki.common.repository.MusicRepository
import com.niki.common.values.FragmentTag
import com.niki.music.R
import com.niki.music.common.ui.SongAdapter
import com.niki.music.common.views.IView
import com.niki.music.databinding.FragmentSearchResultBinding
import com.p1ay1s.dev.base.findFragmentHost
import com.p1ay1s.dev.ui.PreloadLayoutManager
import com.p1ay1s.dev.viewbinding.ViewBindingFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class ResultFragment : ViewBindingFragment<FragmentSearchResultBinding>(), IView,
    SearchView.OnQueryTextListener {
    private val resultViewModel: ResultViewModel by viewModels<ResultViewModel>()

    private lateinit var songAdapter: SongAdapter
    private lateinit var baseLayoutManager: PreloadLayoutManager
    private lateinit var itemAnimation: Animation

    val searchView: SearchView
        get() = binding.searchViewResult

    private var mIsLoading = false
    private var mHandleJob: Job? = null

    override fun FragmentSearchResultBinding.initBinding() {
        initValues()
        handle()

        with(recyclerViewResult) {
            adapter = songAdapter
            layoutManager = baseLayoutManager
            animation = itemAnimation

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    recyclerView.apply {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            if (!canScrollVertically(-1) && !mIsLoading) {
//                                mIsLoading = true
//                                listenViewModel.sendIntent(ListenIntent.GetTopPlaylists())
                            }
                        }
                    }
                }
            })
        }

        searchView.apply {
            setOnQueryTextListener(this@ResultFragment)

            setOnCloseListener {
                findFragmentHost()?.navigate(FragmentTag.PREVIEW_FRAGMENT)
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        MusicRepository.run {
            if (searchPlaylist.isNotEmpty() && songAdapter.currentList.isEmpty())
                songAdapter.submitList(searchPlaylist)
        }
    }

    private fun initValues() {
        songAdapter = SongAdapter()
        baseLayoutManager = PreloadLayoutManager(
            requireActivity(),
            LinearLayoutManager.VERTICAL,
            4
        )
        itemAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in_anim)
    }

    override fun handle() = resultViewModel.apply {
        mHandleJob?.cancel()
        mHandleJob = lifecycleScope.launch {
            uiEffectFlow
                .collect {
                    when (it) {
                        is ResultEffect.SearchSongsState -> {
                            if (it.isSuccess)
                                songAdapter.submitList(MusicRepository.searchPlaylist)
                        }
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mHandleJob?.cancel()
        mHandleJob = null
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        searchAction(query)
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        searchAction(newText)
        return true
    }

    private fun searchAction(str: String?) {
        resultViewModel.sendIntent(ResultIntent.KeywordsChanged(str ?: " "))

        if (!str.isNullOrBlank()) {
            resultViewModel.sendIntent(ResultIntent.SearchSongs)
        }
    }
}