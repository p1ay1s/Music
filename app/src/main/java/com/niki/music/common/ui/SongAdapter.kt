package com.niki.music.common.ui

import android.view.View
import androidx.recyclerview.widget.DiffUtil
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.repository.dataclasses.song.SongInfo
import com.niki.common.utils.setSongDetails
import com.niki.music.databinding.LayoutSongBinding
import com.p1ay1s.impl.ui.ViewBindingListAdapter
import com.p1ay1s.util.ImageSetter.setRadiusImgView

interface SongAdapterListener {
    fun onPlayMusic(list: List<Song>)
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

//    override fun getItemViewType(position: Int): Int {
//        listener // TODO
//        return super.getItemViewType(position)
//    }

    fun setSongAdapterListener(l: SongAdapterListener) {
        listener = l
    }

    fun removeSongAdapterListener() {
        listener = null
    }

    private fun isExplicit(mark: Long?) =
        if (mark != null) (mark and EXPLICIT) == EXPLICIT else false

    override fun LayoutSongBinding.onBindViewHolder(data: Song, position: Int) {
        root.setOnClickListener {
            listener?.onPlayMusic(getRelocatedList(data))
        }

        if (showImage)
            cover.setRadiusImgView(data.al.picUrl, enableCache = enableCache)
        else
            cover.visibility = View.GONE

        songName.text = data.name

        if (showDetails) {
            songDetails.setSongDetails(data)
            if (isExplicit(data.mark)) {
                explicit.visibility = View.VISIBLE
            }
        }

        more.setOnClickListener {
            listener?.onMoreClicked(data)
        }
    }

    private fun getRelocatedList(startSong: Song): List<Song> {
        val list = mutableListOf<Song>()
        currentList.apply {
            if (isEmpty()) return emptyList()

            val index = indexOf(startSong)
            list.addAll(subList(index, size)) // 不用减一
            list.addAll(subList(0, index))
        }
        return list.toList()
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