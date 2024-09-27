package com.niki.music.search

import android.os.Build
import android.widget.SearchView
import androidx.annotation.RequiresApi
import com.niki.music.search.preview.SearchPreviewFragment
import com.niki.music.search.result.SearchResultFragment
import com.p1ay1s.extensions.views.ContainerFragment

const val SEARCH_PREVIEW = "search preview"
const val SEARCH_RESULT = "search result"

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class SearchFragment : ContainerFragment(
    linkedMapOf(
        SEARCH_PREVIEW to SearchPreviewFragment(),
        SEARCH_RESULT to SearchResultFragment()
    )
) {
    lateinit var previewSearchView: SearchView
    lateinit var resultSearchView: SearchView
}