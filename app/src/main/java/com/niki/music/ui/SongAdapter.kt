package com.niki.music.ui

import android.view.View
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.utils.setSongDetails
import com.niki.music.appVibrator
import com.niki.music.databinding.LayoutSongBinding
import com.p1ay1s.base.extension.loadRadiusImage
import com.p1ay1s.impl.ui.ViewBindingListAdapter

interface SongAdapterListener {
    fun onPlayMusic(list: List<Song>)
    fun onMoreClicked(song: Song)
}

class SongAdapter(
    private val showDetails: Boolean = true,
    private val showImage: Boolean = true,
) : ViewBindingListAdapter<LayoutSongBinding, Song>(SongCallback()) {

    private var listener: SongAdapterListener? = null

    companion object {
        const val EXPLICIT = 1048576L
    }

    fun setSongAdapterListener(l: SongAdapterListener?) {
        listener = l
    }

    private fun isExplicit(mark: Long?) =
        if (mark != null) (mark and EXPLICIT) == EXPLICIT else false

    override fun LayoutSongBinding.onBindViewHolder(data: Song, position: Int) {
        root.run {
            setOnClickListener {
                listener?.onPlayMusic(getRelocatedList(data))
            }

            setOnLongClickListener {
                listener?.onMoreClicked(data)
                false // true -> 还会触发 onclick
            }

            updateLayoutParams {
                height = (0.08 * resources.displayMetrics.heightPixels).toInt()
            }
        }

        if (showImage)
            cover.loadRadiusImage(data.al.picUrl)
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