package com.niki.music.listen.ui

import androidx.recyclerview.widget.DiffUtil
import com.niki.common.repository.dataclasses.Playlist
import com.niki.common.repository.dataclasses.Song
import com.niki.music.appLoadingDialog
import com.niki.music.common.ILoad
import com.niki.music.databinding.LayoutTopPlaylistBinding
import com.niki.music.models.PlayerModel
import com.p1ay1s.base.extension.toast
import com.p1ay1s.base.log.logE
import com.p1ay1s.impl.ui.ViewBindingListAdapter
import com.p1ay1s.util.ImageSetter.setRadiusImgView

var currentTopPlaylist: Playlist? = null
var currentTopSongs: List<Song>? = null
var currentTopHasMore = true
var isTopLoading = false
var currentTopPage = 0
const val PLAYLIST_SONGS_LIMIT = 15

class TopPlaylistAdapter(
    val callback: () -> Unit
) : ViewBindingListAdapter<LayoutTopPlaylistBinding, Playlist, List<Song>>(TopPlaylistCallback()),
    ILoad {
    private val playerModel by lazy { PlayerModel() }

    override fun LayoutTopPlaylistBinding.onBindViewHolder(data: Playlist, position: Int) {
        playlist = data

        cover.setRadiusImgView(data.coverImgUrl, radius = 55)
        root.setOnClickListener {
            if (currentTopPlaylist?.id == data.id) { // 如果是相同的就直接打开
                this@TopPlaylistAdapter.callback()
                return@setOnClickListener
            }
            currentTopPlaylist = data
            loadFirstPage(data.id)
        }
    }

    private fun loadFirstPage(
        id: String,
    ) {
        startWaiting()
        playerModel.getSongsFromPlaylist(
            id,
            PLAYLIST_SONGS_LIMIT,
            0,
            {
                val songs = it.songs
                currentTopPage = 1
                currentTopHasMore = songs.isNotEmpty()
                currentTopSongs = songs
                endWaiting()
                this.callback() // 启动 top playlist fragment
            },
            { _, _ ->
                toast("网络错误, 请重试")
                endWaiting()
            })
    }

    override fun startWaiting() {
        appLoadingDialog?.show()
        isTopLoading = true
    }

    override fun endWaiting() {
        appLoadingDialog?.dismiss()
        isTopLoading = false
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