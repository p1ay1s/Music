package com.niki.music.common.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.utils.setSingerName
import com.niki.music.MainActivity
import com.niki.music.R
import com.niki.music.databinding.FragmentSongDetailBinding
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

    lateinit var binding: FragmentSongDetailBinding

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
        binding.run {
            root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bar))
            song = targetSong
            singerName.setSingerName(targetSong)
            cover.setRadiusImgView(targetSong.al.picUrl, radius = 30)
            cover.setOnClickListener {
                (activity as MainActivity).onSongPass(listOf(targetSong))
            }
        }
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