package com.niki.music.listen.ui

import android.os.Build
import android.view.animation.AnimationUtils
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.niki.music.R
import com.niki.music.common.commonViewModels.MusicViewModel
import com.niki.music.common.ui.SongAdapter
import com.niki.music.databinding.LayoutHotPlaylistBinding
import com.niki.music.model.Song
import com.niki.music.model.Tag
import com.niki.utils.base.appContext
import com.niki.utils.base.logE
import com.niki.utils.base.ui.BaseAdapter
import com.niki.utils.base.ui.BaseLayoutManager

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class HotPlaylistAdapter(
    private val musicViewModel: MusicViewModel,
    private val callback: (Tag) -> Unit
) : BaseAdapter<LayoutHotPlaylistBinding, Tag, List<List<Song>>>(HotPlaylistCallback()) {

    companion object {
        const val PRELOAD_SIZE = 5
    }

    override fun LayoutHotPlaylistBinding.onBindViewHolder(bean: Tag, position: Int) {
        if (bean.name.isBlank()) {
            removeItem(position)
            logE(TAG, "删除了无名 item")
            return
        }

        val songAdapter = SongAdapter(musicViewModel, showDetails = false)
        val baseLayoutManager = BaseLayoutManager(
            appContext,
            LinearLayoutManager.VERTICAL,
            PRELOAD_SIZE
        )

        recyclerViewTag.apply {
            adapter = songAdapter
            layoutManager = baseLayoutManager
            animation = AnimationUtils.loadAnimation(context, R.anim.fade_in_anim)
            addItemDecoration(
                DividerItemDecoration(
                    appContext,
                    DividerItemDecoration.VERTICAL
                )
            )
        }

        cateName.text = bean.name
        root.setOnClickListener { callback(bean) }

        musicViewModel.getSongsFromPlaylist(bean.id, 3, 0) {
            if (it.isNullOrEmpty()) {
                removeItem(position)
                logE(TAG, "删除了 item ${bean.name}")
            } else {
                songAdapter.submitList(it)
            }
        }

    }

    private fun removeItem(position: Int) =
        with(currentList.toMutableList()) {
            this.removeAt(position)
            submitList(this)
        }

    class HotPlaylistCallback : DiffUtil.ItemCallback<Tag>() {
        override fun areItemsTheSame(oldItem: Tag, newItem: Tag): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Tag, newItem: Tag): Boolean {
            return oldItem.id == newItem.id
        }
    }
}