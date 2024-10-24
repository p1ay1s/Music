package com.niki.music.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.utils.getLargeRandomNum
import com.niki.common.utils.setSingerName
import com.niki.common.utils.toPlaylist
import com.niki.music.MainActivity
import com.niki.music.R
import com.niki.music.appLoadingDialog
import com.niki.music.databinding.FragmentSongDetailBinding
import com.niki.music.listen.top.PlaylistFragment
import com.niki.music.viewModel.MainViewModel
import com.p1ay1s.base.extension.toast
import com.p1ay1s.util.ImageSetter.setRadiusImgView

fun Fragment.showSongDetail(song: Song) {
    val fragment = SongDetailDialogFragment(song)
    fragment.show(parentFragmentManager, "SONG_DETAIL")
}

class SongDetailDialogFragment(private val targetSong: Song) :
    BottomSheetDialogFragment(R.layout.layout_search_bar) {

    companion object {
        const val HEIGHT_PERCENT = 0.75
    }

    private lateinit var mainViewModel: MainViewModel
    lateinit var binding: FragmentSongDetailBinding
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSongDetailBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        binding.run {
            root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bar))
            song = targetSong
            singerName.setSingerName(targetSong)
            cover.setRadiusImgView(targetSong.al.picUrl, radius = 30)
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

    override fun onStart() {
        super.onStart()
        setupBottomSheetBehavior()
    }

    private fun setupBottomSheetBehavior() = dialog?.apply {
        findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) // view
            ?.let { bottomSheet ->
                with(BottomSheetBehavior.from(bottomSheet)) {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                }
                bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
                    height = (resources.displayMetrics.heightPixels * HEIGHT_PERCENT).toInt()
                }
            }
    }
}