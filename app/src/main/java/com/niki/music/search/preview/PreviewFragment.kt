package com.niki.music.search.preview

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.niki.common.values.FragmentTag
import com.niki.music.common.views.IView
import com.niki.music.databinding.FragmentSearchPreviewBinding
import com.niki.music.search.result.ResultFragment
import com.niki.music.searchIndex
import com.p1ay1s.dev.base.findFragmentHost
import com.p1ay1s.dev.viewbinding.ViewBindingFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class PreviewFragment : ViewBindingFragment<FragmentSearchPreviewBinding>(), IView {

    private var mHandleJob: Job? = null

    override fun FragmentSearchPreviewBinding.initBinding() {
        handle()

        searchViewPreview.setOnSearchClickListener {
            searchViewPreview.isIconified = true

            findFragmentHost()?.let {
                searchIndex = FragmentTag.RESULT_FRAGMENT
                if (!it.navigate(FragmentTag.RESULT_FRAGMENT))
                    it.add(FragmentTag.RESULT_FRAGMENT, ResultFragment())
                (it.getCurrentFragment() as ResultFragment).searchView.apply {
                    isIconified = false
                    requestFocus()
                }
            }
        }
    }

    override fun handle(): Any {
        mHandleJob?.cancel()
        mHandleJob = lifecycleScope.launch { }
        return Any()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mHandleJob?.cancel()
        mHandleJob = null
    }
}