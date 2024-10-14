package com.niki.music.search.result

import android.R
import android.app.SearchManager
import android.database.MatrixCursor
import android.provider.BaseColumns
import android.widget.CursorAdapter
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.SimpleCursorAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.values.FragmentTag
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
import com.p1ay1s.base.log.logE
import com.p1ay1s.base.ui.PreloadLayoutManager
import com.p1ay1s.impl.ViewBindingFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ResultFragment : ViewBindingFragment<FragmentSearchResultBinding>(), IView,
    SearchView.OnQueryTextListener, SongAdapterListener {
    private lateinit var resultViewModel: ResultViewModel
    private val mainViewModel: MainViewModel by activityViewModels<MainViewModel>()

    private lateinit var songAdapter: SongAdapter
    private lateinit var baseLayoutManager: PreloadLayoutManager

    val searchView: SearchView
        get() = binding.searchViewResult

    override fun FragmentSearchResultBinding.initBinding() {
        resultViewModel = ViewModelProvider(requireActivity())[ResultViewModel::class.java]

        lifecycleScope.launch {
            while (true) {
                delay(1000)
                if (searchView.suggestionsAdapter == null)
                    logE("####", "null")
                else
                    logE("####", searchView.suggestionsAdapter.cursor.count.toString())
            }
        }

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

        searchViewResult.apply {
            setOnQueryTextListener(this@ResultFragment)

            setOnCloseListener {
                findFragmentHost()?.navigate(FragmentTag.PREVIEW_FRAGMENT)
                false
            }

//            setOnFocusChangeListener { _, h ->
//                if (h) {
//                    tryShowSuggest()
//                } else {
//                    suggestionsAdapter = null
//                }
//            }
        }
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
        lifecycleScope.apply {
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
                    showSuggest(resultViewModel.state.hotKeywords)
                }
            }
            launch {
                resultViewModel.run {
                    uiEffectFlow
                        .collect {
                            when (it) {
                                is ResultEffect.KeywordSuccessEffect -> showSuggest(state.suggestKeywords)

                                is ResultEffect.KeywordsFailedEffect -> showSuggest(state.hotKeywords)
                            }
                        }
                }
            }
        }
    }

    private fun showSuggest(list: List<String>?) {
        if (list == null) {
//            searchView.suggestionsAdapter = null
            return
        }
        val cursor = MatrixCursor(
            arrayOf(
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1
            )
        )

        list.forEachIndexed { index, suggestion ->
            cursor.addRow(arrayOf(index, suggestion))
        }

        val from = arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1)
        val to = intArrayOf(R.id.text1)
        searchView.suggestionsAdapter = SimpleCursorAdapter(
            requireContext(),
            R.layout.simple_dropdown_item_1line,
            cursor,
            from,
            to,
            CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        songAdapter.removeSongAdapterListener()
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        searchAction(query)
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        resultViewModel.sendIntent(ResultIntent.KeywordsChanged(newText ?: " "))
        return true
    }

    private fun searchAction(str: String?) {
        resultViewModel.sendIntent(ResultIntent.KeywordsChanged(str ?: " "))
        resultViewModel.sendIntent(ResultIntent.SearchSongs)
    }

    override fun onPlayMusic(song: Song) {
        mainViewModel.sendIntent(MainIntent.TryPlaySong(song))
    }

    override fun onMoreClicked(song: Song) {
        toast("more -> ${song.name}")
    }
}