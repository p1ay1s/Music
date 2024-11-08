package com.niki.music.listen.top

import androidx.recyclerview.widget.DiffUtil
import com.niki.common.repository.dataclasses.playlist.Playlist
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.utils.setHorizontalMargins
import com.niki.common.utils.setSize
import com.niki.music.appLoadingDialog
import com.niki.music.databinding.LayoutTopPlaylistBinding
import com.niki.music.model.PlayerModel
import com.p1ay1s.base.extension.loadRadiusImage
import com.p1ay1s.base.extension.toast
import com.p1ay1s.vbclass.ui.ViewBindingListAdapter

interface TopPlaylistAdapterListener {

    /**
     * 资源准备完成后回调
     */
    fun onReady(playlist: Playlist, songs: List<Song>)
}

class TopPlaylistAdapter :
    ViewBindingListAdapter<LayoutTopPlaylistBinding, Playlist>(TopPlaylistCallback()) {

    companion object {
        const val PLAYLIST_SONGS_LIMIT = 15

        private const val NORMAL_MARGIN = 0.03
        private const val ROOT_SIZE = 0.5 - NORMAL_MARGIN * 2
    }

    private var listener: TopPlaylistAdapterListener? = null
    private val playerModel by lazy { PlayerModel() }

    private var isLoading = false

    override fun LayoutTopPlaylistBinding.onBindViewHolder(data: Playlist, position: Int) {
        playlist = data
        val w = root.resources.displayMetrics.widthPixels

        root.setSize(width = (w * ROOT_SIZE).toInt())
        root.setHorizontalMargins((w * NORMAL_MARGIN).toInt())

        cover.setSize((w * ROOT_SIZE).toInt())

        cover.loadRadiusImage(data.coverImgUrl, radius = 55)
        root.setOnClickListener {
            loadFirstPage(data)
        }
    }

    private fun loadFirstPage(
        playlist: Playlist,
    ) {
        if (isLoading) return
        startWaiting()
        playerModel.getSongsFromPlaylist(
            playlist.id,
            PLAYLIST_SONGS_LIMIT,
            0,
            {
                endWaiting()
                listener?.onReady(playlist, it.songs) // 启动 top playlist fragment
            },
            { _, _ ->
                toast("网络错误, 请重试")
                endWaiting()
            })
    }

    fun setOnTopPlaylistAdapterListener(listener: TopPlaylistAdapterListener?) {
        this.listener = listener
    }

    private fun startWaiting() {
        appLoadingDialog?.show()
        isLoading = true
    }

    private fun endWaiting() {
        appLoadingDialog?.dismiss()
        isLoading = false
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