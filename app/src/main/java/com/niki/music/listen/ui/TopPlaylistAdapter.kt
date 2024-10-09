package com.niki.music.listen.ui

import androidx.recyclerview.widget.DiffUtil
import com.niki.music.databinding.LayoutTopPlaylistBinding
import com.niki.common.repository.dataclasses.Playlist
import com.niki.common.repository.dataclasses.Song
import com.p1ay1s.dev.util.ImageSetter.setRadiusImgView
import com.p1ay1s.dev.viewbinding.ui.ViewBindingListAdapter

class TopPlaylistAdapter(
    val callback: (Playlist) -> Unit
) : ViewBindingListAdapter<LayoutTopPlaylistBinding, Playlist, List<Song>>(TopPlaylistCallback()) {

    override fun LayoutTopPlaylistBinding.onBindViewHolder(data: Playlist, position: Int) {
        playlist = data
//        loadContent(bean, collector)
        cover.setRadiusImgView(data.coverImgUrl, radius = 55)
        root.setOnClickListener {
            callback(data)
        }
    }

    class TopPlaylistCallback : DiffUtil.ItemCallback<Playlist>() {
        override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem.id == newItem.id
        }
    }
}