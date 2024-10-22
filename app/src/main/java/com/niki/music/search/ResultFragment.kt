package com.niki.music.search

import android.widget.LinearLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.niki.common.repository.dataclasses.song.Song
import com.niki.music.MainActivity
import com.niki.music.appFadeInAnim
import com.niki.music.databinding.FragmentResultBinding
import com.niki.music.search.ui.SearchBar
import com.niki.music.search.ui.SearchBarListener
import com.niki.music.ui.SongAdapter
import com.niki.music.ui.SongAdapterListener
import com.niki.music.ui.showSongDetail
import com.p1ay1s.base.extension.addLineDecoration
import com.p1ay1s.base.extension.addOnLoadMoreListener_V
import com.p1ay1s.base.ui.PreloadLayoutManager
import com.p1ay1s.impl.ViewBindingFragment
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ResultFragment : ViewBindingFragment<FragmentResultBinding>() {
    private lateinit var resultViewModel: ResultViewModel

    private lateinit var songAdapter: SongAdapter
    private lateinit var baseLayoutManager: PreloadLayoutManager

    private val searchBar: SearchBar
        get() = binding.searchBar

    override fun FragmentResultBinding.initBinding() {
        resultViewModel = ViewModelProvider(requireActivity())[ResultViewModel::class.java]

        initValues()
        handle()

        with(recyclerViewResult) {
            adapter = songAdapter
            layoutManager = baseLayoutManager
            animation = appFadeInAnim

            addLineDecoration(requireActivity(), LinearLayout.VERTICAL)
            addOnLoadMoreListener_V(1) {
                resultViewModel.sendIntent(ResultIntent.SearchSongs)
            }
        }

        searchBar.init()
        searchBar.listener = SearchBarListenerImpl()
    }

    private fun initValues() {
        songAdapter = SongAdapter()
        songAdapter.setSongAdapterListener(SongAdapterListenerImpl())
        baseLayoutManager = PreloadLayoutManager(
            requireActivity(),
            LinearLayoutManager.VERTICAL,
            4
        )
    }

    private fun handle() = resultViewModel.apply {
        lifecycleScope.apply {
            observeState {
                launch {
                    map { it.songList }.distinctUntilChanged().collect {
                        if (it == null)
                            songAdapter.submitList(emptyList())
                        else
                            songAdapter.submitList(it)
                    }
                }
                launch {
                    map { it.hotKeywords }.distinctUntilChanged().filterNotNull().collect {
                        searchBar.showDefaultList(it) // 接受并给 searchBar 的热词初始化
                    }
                }
                launch {
                    uiEffectFlow
                        .collect {
                            when (it) {
                                is ResultEffect.KeywordSuccessEffect -> {
                                    searchBar.setSuggestions(
                                        state.suggestKeywords
                                    )
                                }

                                is ResultEffect.KeywordsFailedEffect -> searchBar.showDefaultList() // 失败的时候展示默认
                            }
                        }
                }
            }
        }
    }



    override fun onDestroyView() {
        searchBar.listener = null
        super.onDestroyView() // <---- 此方法后访问 binding 会出错
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

    inner class SearchBarListenerImpl : SearchBarListener {
        override fun onContentChanged(keywords: String) {
            resultViewModel.sendIntent(ResultIntent.KeywordsChanged(keywords))
        }

        override fun onSubmit(keywords: String) {
            resultViewModel.sendIntent(ResultIntent.KeywordsChanged(keywords))
            resultViewModel.sendIntent(ResultIntent.SearchSongs)
        }
    }
}