package com.niki.music.listen.ui

import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import com.niki.common.repository.dataclasses.playlist.Playlist
import com.niki.common.repository.dataclasses.song.Song
import com.niki.music.appLoadingDialog
import com.niki.music.common.ILoad
import com.niki.music.databinding.LayoutTopPlaylistBinding
import com.niki.music.models.PlayerModel
import com.p1ay1s.base.extension.toast
import com.p1ay1s.impl.ui.ViewBindingListAdapter
import com.p1ay1s.util.ImageSetter.setRadiusImgView

var currentTopPlaylist: Playlist? = null
var currentTopSongs: List<Song>? = null
var currentTopHasMore = true
var isTopLoading = false
var lastIsSuccess = false
var currentTopPage = 0
const val PLAYLIST_SONGS_LIMIT = 15

/**
 * 用于实现点击居中 item 时打开 fragment , 否则滚动
 */
interface TopPlaylistAdapterListener {
    /**
     * 是否继续加载
     */
    fun onContact(position: Int): Boolean

    /**
     * 资源准备完成后回调
     */
    fun onReady()
}

class TopPlaylistAdapter :
    ViewBindingListAdapter<LayoutTopPlaylistBinding, Playlist, List<Song>>(TopPlaylistCallback()),
    ILoad {

    private var listener: TopPlaylistAdapterListener? = null
    private val playerModel by lazy { PlayerModel() }

    override fun LayoutTopPlaylistBinding.onBindViewHolder(data: Playlist, position: Int) {
        root.updateLayoutParams {
            val w = root.resources.displayMetrics.widthPixels
            width = (w * 0.83).toInt()
        }

        playlist = data

        cover.setRadiusImgView(data.coverImgUrl, radius = 55)
        root.setOnClickListener {
            onItemClick(data, position)
        }
    }

    private fun onItemClick(data: Playlist, position: Int) {
        val shouldKeepOn = listener?.onContact(position) ?: false
        if (shouldKeepOn) {
            if (currentTopPlaylist?.id == data.id && lastIsSuccess) { // 如果是相同的就直接打开
                listener?.onReady()
                return
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
                lastIsSuccess = true
                val songs = it.songs
                currentTopPage = 1
                currentTopHasMore = songs.isNotEmpty()
                currentTopSongs = songs
                endWaiting()
                listener?.onReady() // 启动 top playlist fragment
            },
            { _, _ ->
                lastIsSuccess = false
                toast("网络错误, 请重试")
                endWaiting()
            })
    }

    fun setOnTopPlaylistAdapterListener(listener: TopPlaylistAdapterListener) {
        this.listener = listener
    }

    fun removeTopPlaylistAdapterListener() {
        this.listener = null
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