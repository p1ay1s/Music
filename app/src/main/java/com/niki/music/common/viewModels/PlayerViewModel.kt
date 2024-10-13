//package com.niki.music.common.viewModels
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.os.Build
//import androidx.annotation.RequiresApi
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import com.niki.common.repository.dataclasses.song.Song
//import com.niki.common.values.BroadCastMsg
//import com.niki.music.common.viewModels.MainViewModel.Companion.LOOP
//import com.niki.music.models.PlayerModel
//import com.p1ay1s.base.appContext
//
//
///**
// * TODO 不需要广播接受！！此处也进行广播发送，全部交给音乐服务！！！
// */
//@RequiresApi(Build.VERSION_CODES.TIRAMISU)
//class PlayerViewModel : ViewModel() {
//    private val playerModel by lazy { PlayerModel() }
//
//    private lateinit var currentPlayList: MutableList<Song>
//    var currentSong = MutableLiveData<Song>()
//
//    private var filter: IntentFilter = IntentFilter()
//    private lateinit var receiver: MusicReceiver
//
//    var updateMusicProgress: (() -> Unit)? = null
//    var seekTo: ((Int) -> Unit)? = null
//    var playMusic: ((String) -> Unit)? = null
//    var switchStatus: (() -> Unit)? = null
//
//    private var currentIndex: Int = 0
//
//    val isPlaying = MutableLiveData<Boolean>()
//    val songPosition = MutableLiveData<Int>()
//    var playMode = MutableLiveData<Int>()
//
//    init {
////        with(BroadCastMsg) {
////            filter.addAction(NEW_STATE)
////            filter.addAction(NEW_PROGRESS)
////            filter.addAction(NEXT)
////            filter.addAction(PREVIOUS)
////        }
////
////        receiver = MusicReceiver()
////        appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
//
//        /**
//         * TODO 从pref中获取
//         */
//        currentPlayList = mutableListOf()
//        playMode.value = LOOP
//    }
//
//
//
//    fun checkSongAbility(song: Song) {
//        playerModel.checkSongAbility(song.id,{ data->
//            if (data.code == 200) {
//                getSongInfo(song.id!!) {
//                    // TODO 播放
//                }
//            }
//        },{_,_->})
//    }
//
////
////    inner class MusicReceiver : BroadcastReceiver() {
////        override fun onReceive(context: Context?, intent: Intent?) {
////            with(BroadCastContents) {
////                when (intent?.action) {
////                    NEW_STATE -> {
////                        val status = intent.getBooleanExtra("status", false)
////                        if (isPlaying.value != status)
////                            isPlaying.value = status
////                    }
////
////                    NEW_PROGRESS -> {
////                        val progress = intent.getIntExtra("progress", 0)
////                        songPosition.value = progress
////                    }
////
////                    NEXT_ONE -> {
////                    }
////
////                    PREVIOUS_ONE -> {
////                    }
////                }
////
////            }
////        }
////    }
//}