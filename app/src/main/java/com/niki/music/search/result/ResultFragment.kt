package com.niki.music.search.result

import android.os.Build
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.niki.common.repository.dataclasses.Song
import com.niki.common.values.FragmentTag
import com.niki.music.MainActivity
import com.niki.music.appFadeInAnim
import com.niki.music.common.ui.SongAdapter
import com.niki.music.common.ui.SongAdapterListener
import com.niki.music.common.viewModels.MainViewModel
import com.niki.music.common.views.IView
import com.niki.music.databinding.FragmentSearchResultBinding
import com.niki.music.intents.MainIntent
import com.p1ay1s.base.extension.addLineDecoration
import com.p1ay1s.base.extension.addOnLoadMoreListener_V
import com.p1ay1s.base.extension.findFragmentHost
import com.p1ay1s.base.extension.toast
import com.p1ay1s.base.ui.PreloadLayoutManager
import com.p1ay1s.impl.ViewBindingFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ResultFragment : ViewBindingFragment<FragmentSearchResultBinding>(), IView,
    SearchView.OnQueryTextListener, SongAdapterListener {
    private lateinit var resultViewModel: ResultViewModel

    private lateinit var songAdapter: SongAdapter
    private lateinit var baseLayoutManager: PreloadLayoutManager

    val searchView: SearchView
        get() = binding.searchViewResult

    private var mIsLoading = false
    private var searchSongsJob: Job? = null

    private val mainViewModel: MainViewModel by activityViewModels<MainViewModel>()

    override fun FragmentSearchResultBinding.initBinding() {
        resultViewModel = ViewModelProvider(requireActivity())[ResultViewModel::class.java]

        initValues()
        handle()

        with(recyclerViewResult) {
            adapter = songAdapter
            layoutManager = baseLayoutManager
            animation = appFadeInAnim

            addLineDecoration(requireActivity(), LinearLayout.VERTICAL)

            addOnLoadMoreListener_V(1) {
//                if(!mIsLoading) // <- 移至函数内
//                    return@setOnLoadMoreListener // TODO
            }
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
//        MusicRepository.run {
//            if (searchPlaylist.isNotEmpty() && songAdapter.currentList.isEmpty())
//                songAdapter.submitList(searchPlaylist)
//        }
    }

    private fun initValues() {
        songAdapter = SongAdapter()
        songAdapter.setSongAdapterListener(this)
        baseLayoutManager = PreloadLayoutManager(
            requireActivity(),
            LinearLayoutManager.VERTICAL,
            4
        )
    }

    override fun handle() = resultViewModel.observeState {
        lifecycleScope.launch {
            map { it.songList }.distinctUntilChanged().collect {
                if (it == null)
                    songAdapter.submitList(emptyList())
                else
                    songAdapter.submitList(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        songAdapter.removeSongAdapterListener()
        searchSongsJob?.cancel()
        searchSongsJob = null
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

    override fun onPlayMusic(song: Song) {
        mainViewModel.sendIntent(MainIntent.TryPlaySong(song))
    }

    override fun onMoreClicked(song: Song) {
        toast("more -> ${song.name}")
    }
}