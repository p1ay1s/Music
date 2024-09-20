package com.niki.music.common.ui

import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import com.niki.base.view.ui.BaseAdapter
import com.niki.base.util.ImageSetter.setRadiusImgView
import com.niki.base.util.toast
import com.niki.music.common.intents.MusicIntent
import com.niki.music.common.viewModels.MusicViewModel
import com.niki.music.databinding.LayoutSongBinding
import com.niki.music.model.Song
import com.niki.music.model.SongInfo

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class SongAdapter(
    private val musicViewModel: MusicViewModel,
    private val enableCache: Boolean = false,
    private val showDetails: Boolean = true,
    private val showImage: Boolean = true
) :
    BaseAdapter<LayoutSongBinding, Song, SongInfo>(SongCallback()) {

    companion object {
        const val EXPLICIT = 1048576L
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
                if (song.ar.indexOf(artist) != song.ar.size - 1) {
                    append(" & ")
                }
            }
            append(" • ")
            append(song.al.name)
        }
        text = builder.toString()
    }

    private fun isExplicit(mark: Long?) =
        if (mark != null) (mark and EXPLICIT) == EXPLICIT else false

    override fun LayoutSongBinding.onBindViewHolder(bean: Song, position: Int) {
        root.setOnClickListener {
            musicViewModel.run {
                if (currentList.isNotEmpty())
                    sendIntent(
                        MusicIntent.SetNewSongList(
                            currentList.toMutableList(),
                            currentList.indexOf(bean)
                        )
                    )
                sendIntent(MusicIntent.TryPlaySong(bean.id))
            }
        }

        if (showImage)
            cover.setRadiusImgView(bean.al.picUrl, enableCache = enableCache)
        else
            cover.visibility = View.GONE

        if (showDetails) {
            songDetails.formatDetails(bean)
            if (isExplicit(bean.mark))
                explicit.visibility = View.VISIBLE
        }

        songName.text = bean.name

        more.setOnClickListener {
            // musicViewModel.addSongToNext(song)
            // TODO a menu!
            toast(bean.name)
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