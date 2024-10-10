package com.niki.music.common.ui

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import com.niki.common.repository.dataclasses.Song
import com.niki.common.repository.dataclasses.SongInfo
import com.niki.music.databinding.LayoutSongBinding
import com.p1ay1s.dev.util.ImageSetter.setRadiusImgView
import com.p1ay1s.dev.viewbinding.ui.ViewBindingListAdapter

interface SongAdapterListener {
    fun onPlayMusic(song: Song)
    fun onMoreClicked(song: Song)
}

class SongAdapter(
    private val enableCache: Boolean = false,
    private val showDetails: Boolean = true,
    private val showImage: Boolean = true
) : ViewBindingListAdapter<LayoutSongBinding, Song, SongInfo>(SongCallback()) {

    private var listener: SongAdapterListener? = null

    companion object {
        const val EXPLICIT = 1048576L
    }

    fun setSongAdapterListener(l: SongAdapterListener) {
        listener = l
    }

    private fun TextView.formatDetails(song: Song) {
        visibility = View.VISIBLE
        val builder = StringBuilder()
        builder.apply {
            for (artist in song.ar) {
                if (artist.name.isNotBlank()) {
                    append(artist.name)
                } else {
                    continue
                }
                val index = song.ar.indexOf(artist)
                when (index) { // 效果: a, b, c & d
                    song.ar.size - 1 -> {} // the last
                    song.ar.size - 2 -> append(" & ")
                    else -> append(", ")
                }
            }
            if (song.al.name.isNotBlank()) {
                append(" • ")
                append(song.al.name)
            }
        }
        text = builder.toString()
    }

    private fun isExplicit(mark: Long?) =
        if (mark != null) (mark and EXPLICIT) == EXPLICIT else false

    override fun LayoutSongBinding.onBindViewHolder(data: Song, position: Int) {
        root.setOnClickListener {
            listener?.onPlayMusic(data)
//            appMainViewModel?.run {
//                if (currentList.isNotEmpty())
//                    sendIntent(
//                        MusicIntent.SetNewSongList(
//                            currentList.toMutableList(),
//                            currentList.indexOf(data)
//                        )
//                    )
//                sendIntent(MusicIntent.TryPlaySong(data.id))
//            }
        }

        if (showImage)
            cover.setRadiusImgView(data.al.picUrl, enableCache = enableCache)
        else
            cover.visibility = View.GONE

        if (showDetails) {
            songDetails.formatDetails(data)
            if (isExplicit(data.mark))
                explicit.visibility = View.VISIBLE
        }

        songName.text = data.name

        more.setOnClickListener {
            listener?.onMoreClicked(data)
        }
    }

    class SongCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.id == newItem.id
        }
    }
}