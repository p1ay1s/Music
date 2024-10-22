package com.niki.music.my

import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.niki.common.repository.dataclasses.song.Song
import com.niki.music.MainActivity
import com.niki.music.appFadeInAnim
import com.niki.music.databinding.FragmentMyBinding
import com.niki.music.my.login.LoginFragment
import com.niki.music.ui.SongAdapter
import com.niki.music.ui.SongAdapterListener
import com.niki.music.ui.showSongDetail
import com.p1ay1s.base.extension.addLineDecoration
import com.p1ay1s.base.ui.PreloadLayoutManager
import com.p1ay1s.impl.ViewBindingFragment
import com.p1ay1s.util.ImageSetter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MyFragment : ViewBindingFragment<FragmentMyBinding>() {

    companion object {
        const val CLICK_TO_LOGIN = "点击登录"
        const val NOT_YET_LOGGED_IN = "未登录"
        const val LOGOUT = "登出"
    }

    private lateinit var songAdapter: SongAdapter
    private lateinit var baseLayoutManager: PreloadLayoutManager

    private lateinit var myViewModel: MyViewModel

    private var loginStateJob: Job? = null
    private var likeListJob: Job? = null

    override fun FragmentMyBinding.initBinding() {
        appbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val alpha = (-verticalOffset / appBarLayout.totalScrollRange.toFloat())
            structure.alpha = 1 - alpha * alpha
        }

        // 如果 activity 中没有创建 vm 而使用 activityViewModels 就会是不同的实例
        myViewModel = ViewModelProvider(requireActivity())[MyViewModel::class.java]

        initValues()
        handle()

        with(binding.recyclerView) {
            adapter = songAdapter
            layoutManager = baseLayoutManager
            animation = appFadeInAnim

            addLineDecoration(requireActivity(), LinearLayout.VERTICAL)
        }
    }

    override fun onResume() {
        super.onResume()

        myViewModel.state.run {
            if (likeList == null && isLoggedIn)
                myViewModel.sendIntent(MyIntent.GetLikePlaylist)
        }
    }

    private fun initValues() {
        songAdapter = SongAdapter(
            enableCache = false,
            showDetails = true,
            showImage = false
        )
        songAdapter.setSongAdapterListener(SongAdapterListenerImpl())
        baseLayoutManager = PreloadLayoutManager(
            requireActivity(),
            LinearLayoutManager.VERTICAL,
            4
        )
    }

    private fun handle() = myViewModel.observeState {
        loginStateJob?.cancel()
        loginStateJob = lifecycleScope.launch {
            map { it.loggedInDatas }.distinctUntilChanged().collect {
                if (it == null)
                    setUnLoggedInViews()
                else
                    setLoggedInViews(it)
            }
        }

        likeListJob?.cancel()
        likeListJob = lifecycleScope.launch {
            map { it.likeList }.distinctUntilChanged().filterNotNull().collect {
                songAdapter.submitList(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        songAdapter.setSongAdapterListener(null)
        loginStateJob?.cancel()
        loginStateJob = null
        likeListJob?.cancel()
        likeListJob = null
    }

    private fun setUnLoggedInViews() {
        binding.apply {
            userAvatar.visibility = View.INVISIBLE
            background.visibility = View.INVISIBLE
            nickname.text = NOT_YET_LOGGED_IN
            logout.text = CLICK_TO_LOGIN
            logout.setOnClickListener {
                LoginFragment().show(
                    requireActivity().supportFragmentManager,
                    LoginFragment::class.simpleName!!
                )
            }

            songAdapter.submitList(emptyList())
        }
    }

    private fun setLoggedInViews(data: LoggedInDatas) {
        binding.run {
            nickname.text = data.nickname
            logout.text = LOGOUT
            logout.setOnClickListener {
                myViewModel.sendIntent(MyIntent.Logout)
            }

            ImageSetter.apply {
                userAvatar.setCircleImgView(
                    data.avatarUrl,
                    enableCache = true
                )
                background.setImgView(
                    data.backgroundUrl,
                    enableCache = true
                )
            }

            myViewModel.state.apply {
                if (isLoggedIn && likeList == null)
                    myViewModel.sendIntent(MyIntent.GetLikePlaylist)
            }
        }
    }

    inner class SongAdapterListenerImpl : SongAdapterListener {
        override fun onPlayMusic(list: List<Song>) {
            (activity as MainActivity).onSongPass(list)
//            mainViewModel.sendIntent(MainIntent.TryPlaySong(song))
        }

        override fun onMoreClicked(song: Song) {
            showSongDetail(song)
        }
    }
}