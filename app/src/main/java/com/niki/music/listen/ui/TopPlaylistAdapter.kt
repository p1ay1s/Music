package com.niki.music.listen.ui

import androidx.recyclerview.widget.DiffUtil
import com.niki.music.databinding.LayoutTopPlaylistBinding
import com.niki.music.model.Playlist
import com.niki.music.model.Song
import com.niki.base.util.ImageSetter.setRadiusImgView
import com.niki.base.view.ui.BaseAdapter

class TopPlaylistAdapter(
    val callback: (Playlist) -> Unit
) :
    BaseAdapter<LayoutTopPlaylistBinding, Playlist, List<Song>>(TopPlaylistCallback()) {

    override fun LayoutTopPlaylistBinding.onBindViewHolder(bean: Playlist, position: Int) {
        playlist = bean
//        loadContent(bean, collector)
        cover.setRadiusImgView(bean.coverImgUrl, radius = 55)
        root.setOnClickListener {
            callback(bean)
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