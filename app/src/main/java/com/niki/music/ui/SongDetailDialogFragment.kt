package com.niki.music.ui

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.utils.getLargeRandomNum
import com.niki.common.utils.setSingerName
import com.niki.common.utils.toPlaylist
import com.niki.music.MainActivity
import com.niki.music.R
import com.niki.music.appLoadingDialog
import com.niki.music.appVibrator
import com.niki.music.databinding.FragmentSongDetailBinding
import com.niki.music.listen.top.PlaylistFragment
import com.niki.music.viewModel.MainViewModel
import com.p1ay1s.base.extension.toast
import com.p1ay1s.vbclass.ui.ViewBindingDialogFragment

fun Fragment.showSongDetail(song: Song) {
    val fragment = SongDetailDialogFragment(song)
    fragment.show(parentFragmentManager, "SONG_DETAIL")
    appVibrator?.vibrate(15)
}

class SongDetailDialogFragment(private val song: Song? = null) :
    ViewBindingDialogFragment<FragmentSongDetailBinding>() {

    private lateinit var mainViewModel: MainViewModel
    private var isLoading = false

    private val targetSong: Song
        get() {
            if (song == null) {
                dismiss()
                return Song()
            } else
                return song
        }

    override fun FragmentSongDetailBinding.initBinding() {
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        song = targetSong
        singerName.setSingerName(targetSong)
        cover.loadCover(targetSong.al.picUrl, radius = 30)
        root.setOnClickListener {

        }
        singerName.setOnClickListener { singerName.text.toast() }
        songName.setOnClickListener { songName.text.toast() }
        albumName.setOnClickListener {
            loadAlbumFragment()
        }
        cover.setOnClickListener {
            (activity as MainActivity).onSongPass(listOf(targetSong))
            dismiss()
        }
    }

    private fun loadAlbumFragment() {
        if (isLoading) return
        startWaiting()
        mainViewModel.getSongsFromAlbum(targetSong.al.id) { songs, album ->
            if (!songs.isNullOrEmpty()) {
                try {
                    val num = getLargeRandomNum()
                    mainViewModel.playlistMap[num.toString()] =
                        Pair(album!!.album.toPlaylist(), songs)
                    mainViewModel.host?.pushFragment(
                        num,
                        PlaylistFragment(),
                        R.anim.right_enter,
                        R.anim.fade_out
                    )
                    dismiss()
                } catch (_: Exception) {
                    "该歌曲为单曲".toast()
                }
            } else {
                "加载失败, 请重试".toast()
            }
            endWaiting()
        }
    }

    private fun startWaiting() {
        appLoadingDialog?.show()
        isLoading = true
    }

    private fun endWaiting() {
        appLoadingDialog?.dismiss()
        isLoading = false
    }
}