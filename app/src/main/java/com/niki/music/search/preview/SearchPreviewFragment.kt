package com.niki.music.search.preview

import android.os.Build
import android.widget.SearchView
import androidx.annotation.RequiresApi
import com.niki.music.common.views.IView
import com.niki.music.databinding.FragmentSearchPreviewBinding
import com.niki.music.search.SEARCH_RESULT
import com.niki.music.search.SearchFragment
import com.p1ay1s.extensions.views.ChildFragment

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class SearchPreviewFragment : ChildFragment<FragmentSearchPreviewBinding>(), IView {

    private fun getSearchView(): SearchView? {
        return try {
            ((parentFragment as SearchFragment).resultSearchView)
        } catch (_: Exception) {
            null
        }
    }

    override fun FragmentSearchPreviewBinding.initBinding() {
//        searchButton.setOnClickListener {
//            getControllerView()?.switchToFragment(SEARCH_RESULT)
//            with(getSearchView()) {
//                this?.isIconified = false
//            }
//        }
        searchViewPreview.run {
            (parentFragment as SearchFragment).previewSearchView = this
            setOnSearchClickListener {
                isIconified = true
                getControllerView()?.switchToFragment(SEARCH_RESULT)
                with(getSearchView()) {
                    this?.isIconified = false
                }
            }
//            setOnClickListener {
//                getControllerView()?.switchToFragment(SEARCH_RESULT)
//                with(getSearchView()) {
//                    this?.requestFocus()
//                }
//            }
//            setOnFocusChangeListener { _, hasFocus ->
//                if(hasFocus){
//
//                    val content = getSearchView()?.query
//
//                    if (content.isNullOrBlank()) {
//                        isIconified = true
//                    } else {
//                        isIconified = false
//                        this.setQuery(content, false)
//                    }
//                }
//            }
        }


//        recyclerView.apply {
//            isNestedScrollingEnabled = false
//            adapter = songAdapter
//            layoutManager = baseLayoutManager
//            animation = AnimationUtils.loadAnimation(context, R.anim.fade_in_anim)
//            addItemDecoration(
//                DividerItemDecoration(
//                    requireActivity(),
//                    DividerItemDecoration.VERTICAL
//                )
//            )
//        }

        handle()
    }

    override fun handle(): Any {
        return Any()
    }

    override fun <T> onPassData(receiverIndex: String, data: T?) {

    }

}