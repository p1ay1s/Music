package com.niki.music.search

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.niki.music.common.ui.SongAdapter
import com.niki.music.common.viewModels.MusicViewModel
import com.niki.music.common.views.IView
import com.niki.music.databinding.FragmentSearchBinding
import com.niki.base.view.BaseFragment
import com.niki.base.view.ui.BaseLayoutManager
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class SearchFragment : BaseFragment<FragmentSearchBinding>(), IView {
    private val searchViewModel: SearchViewModel by viewModels<SearchViewModel>()
    private val musicViewModel: MusicViewModel by activityViewModels<MusicViewModel>()

    private lateinit var songAdapter: SongAdapter
    private lateinit var baseLayoutManager: BaseLayoutManager

    override fun FragmentSearchBinding.initBinding() {
        initValues()

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

    override fun handle() = searchViewModel.apply {
        lifecycleScope.launch {
            uiEffectFlow
                .collect {
                    when (it) {
                        is SearchEffect.SearchSongsState ->{}
                    }
                }
        }
    }

    private fun initValues() {
        songAdapter = SongAdapter(musicViewModel)
        baseLayoutManager = BaseLayoutManager(
            requireActivity(),
            LinearLayoutManager.VERTICAL,
            4
        )
    }
}