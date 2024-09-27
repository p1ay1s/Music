package com.niki.music.search.result

import android.os.Build
import android.widget.SearchView
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.niki.music.common.ui.SongAdapter
import com.niki.music.common.viewModels.MusicViewModel
import com.niki.music.common.views.IView
import com.niki.music.databinding.FragmentSearchResultBinding
import com.niki.music.search.SEARCH_PREVIEW
import com.niki.music.search.SearchFragment
import com.p1ay1s.dev.ui.PreloadLayoutManager
import com.p1ay1s.extensions.views.ChildFragment
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class SearchResultFragment : ChildFragment<FragmentSearchResultBinding>(), IView {
    private val searchResultViewModel: SearchResultViewModel by viewModels<SearchResultViewModel>()
    private val musicViewModel: MusicViewModel by activityViewModels<MusicViewModel>()

    private lateinit var songAdapter: SongAdapter
    private lateinit var baseLayoutManager: PreloadLayoutManager

    private fun getSearchView(): SearchView? {
        return try {
            ((parentFragment as SearchFragment).previewSearchView)
        } catch (_: Exception) {
            null
        }
    }

    override fun FragmentSearchResultBinding.initBinding() {
        searchViewResult.run {
            (parentFragment as SearchFragment).resultSearchView = this
            setOnCloseListener {
                getControllerView()?.switchToFragment(SEARCH_PREVIEW)
                if (query.isNotBlank())
                    with(getSearchView()) {
                        this?.requestFocus()
                    }
                true
            }
        }
        initValues()
        handle()
    }

    override fun handle() = searchResultViewModel.apply {
        lifecycleScope.launch {
            uiEffectFlow
                .collect {
                    when (it) {
                        is SearchEffect.SearchSongsState -> {}
                    }
                }
        }
    }

    private fun initValues() {
        songAdapter = SongAdapter(musicViewModel)
        baseLayoutManager = PreloadLayoutManager(
            requireActivity(),
            LinearLayoutManager.VERTICAL,
            4
        )
    }

    override fun <T> onPassData(receiverIndex: String, data: T?) {

    }
}