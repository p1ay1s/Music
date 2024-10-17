package com.niki.music

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.ui.LoadingDialog
import com.niki.common.utils.formatDetails
import com.niki.common.utils.getNewTag
import com.niki.common.values.FragmentTag
import com.niki.music.MusicController.registerMusicReceiver
import com.niki.music.MusicController.releaseMusicController
import com.niki.music.common.ui.BlurTransformation
import com.niki.music.common.ui.button.PlayButton
import com.niki.music.common.ui.button.PlayModeButton
import com.niki.music.common.viewModels.MainViewModel
import com.niki.music.databinding.ActivityMainBinding
import com.niki.music.listen.ui.TopPlaylistFragment
import com.niki.music.my.MyFragment
import com.niki.music.my.login.dismissCallback
import com.niki.music.search.result.ResultFragment
import com.p1ay1s.base.ActivityPreferences
import com.p1ay1s.base.appBaseUrl
import com.p1ay1s.base.appContext
import com.p1ay1s.base.extension.toast
import com.p1ay1s.base.extension.withPermission
import com.p1ay1s.base.ui.FragmentHost
import com.p1ay1s.impl.ViewBindingActivity
import com.p1ay1s.util.ImageSetter.setRadiusImgView
import com.p1ay1s.util.ServiceBuilder.ping
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


var appLoadingDialog: LoadingDialog? = null
var appFadeInAnim: Animation? = null

class MainActivity : ViewBindingActivity<ActivityMainBinding>(),
    ActivityPreferences.TwoClicksListener {

    var binder: RemoteControlService.RemoteControlBinder? = null
    var playerBackground: Drawable? = null

    companion object {
        const val LISTEN_KEY = "LISTEN"
        const val MY_KEY = "MY"
        const val SEARCH_KEY = "SEARCH"

        const val CURRENT = "current"
        const val PREVIOUS = "previous"

        const val BOTTOM_NAV_WEIGHT = 0.1
    }

    private var canPostNotification = false

    private var exitJob: Job? = null

    private var oneMoreClickToExit = false

    private lateinit var mainViewModel: MainViewModel

    /**
     * 用于导航
     */
    private var listenIndex = FragmentTag.LISTEN_FRAGMENT
    private var myIndex = FragmentTag.MY_FRAGMENT
    private var searchIndex = FragmentTag.PREVIEW_FRAGMENT

    /**
     * 用于处理 bottom navigation view 的点击事件
     *
     * 比如点击同一个选项回退上一界面等
     */
    private var currentTag = FragmentTag.LISTEN_FRAGMENT
    private var previousTag = FragmentTag.LISTEN_FRAGMENT

    private val playerBehavior
        get() = BottomSheetBehavior.from(binding.player)

    override fun ActivityMainBinding.initBinding() {
        // 非得要 activity 的上下文
        appLoadingDialog = LoadingDialog(this@MainActivity)
        MusicController.listener = MusicControllerListenerImpl()
        mainViewModel = ViewModelProvider(this@MainActivity)[MainViewModel::class.java]
        appFadeInAnim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_in)
        registerMusicReceiver()

        checkServerUsability()
        tryShowRemoteControl()

        mainViewModel.run {
            if (fragmentHost == null) {
                fragmentHost =
                    fragmentHostView.create(supportFragmentManager, fragmentMap)
                fragmentHost?.setOnIndexChangeListener(OnIndexChangeListenerImpl())
            } else {
                fragmentHostView.restore(fragmentHost!!, supportFragmentManager)
                fragmentHost?.setOnIndexChangeListener(OnIndexChangeListenerImpl())
            }
        }

        bottomNavigation.setSwitchHandler()

        playerBehavior.apply {
            state = BottomSheetBehavior.STATE_COLLAPSED
            addBottomSheetCallback(BottomSheetCallbackImpl())
            player.setOnClickListener {
                if (this.state != BottomSheetBehavior.STATE_EXPANDED)
                    this.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        seekBar.setOnSeekBarChangeListener(OnSeekBarChangeListenerImpl())

        play.setOnClickListener {
            MusicController.run {
                if (isPlaying) pause() else play()
            }
        }
        previous.setOnClickListener {
            MusicController.previous()
        }
        next.setOnClickListener {
            MusicController.next()
        }
        playMode.setOnClickListener {
            MusicController.changePlayMode()
        }
        playerBackground = player.background
        setViewsLayoutParams()
    }

    /**
     * 不用约束布局只能用 layout params 设置
     */
    private fun setViewsLayoutParams() {
        binding.apply {
            val parentHeight = root.resources.displayMetrics.heightPixels
            val parentWeight = root.resources.displayMetrics.widthPixels
            val navHeight = (parentHeight * BOTTOM_NAV_WEIGHT).toInt()
            fragmentHostView.updateLayoutParams {
                height = parentHeight - navHeight
            }
            bottomNavigation.updateLayoutParams {
                height = navHeight
            }
            cover.updateLayoutParams {
                width = (0.85 * parentWeight).toInt()
                height = (0.85 * parentWeight).toInt()
            }
            playerBehavior.peekHeight = navHeight * 2
        }
    }

    /**
     * 检查 localhost 是否可用
     */
    private fun checkServerUsability() {
        appLoadingDialog?.show()
        ping(appBaseUrl) { isSuccess ->
            if (!isSuccess)
                toast("服务器连接失败")
            appLoadingDialog?.dismiss()
        }
    }

    /**
     * 检查通知权限, 有权限则启动通知栏播放器
     */
    private fun tryShowRemoteControl() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            withPermission(POST_NOTIFICATIONS) {
                canPostNotification = it
                if (!it)
                    toast("未授予通知权限, 无法启用状态栏控制")
                else
                    startRemoteControl()
            }
        else
            toast("本设备不支持状态栏控制")
    }

    /**
     * 设置导航的处理逻辑
     */
    private fun BottomNavigationView.setSwitchHandler() {
        setOnItemSelectedListener { item ->
            val newIndex = getIndex(item.itemId)
            currentTag = getNewTag(newIndex) // 用 index 判断属于哪个页面

            if (previousTag == currentTag) { // 如果点击了相同的按钮则尝试回退到上一层 fragment
                handleBackPress(false)
            } else { // 否则判断方位并导航
                mainViewModel.fragmentHost?.navigate(
                    newIndex,
                    R.anim.fade_in,
                    R.anim.fade_out
                )
            }

            previousTag = currentTag // 更新旧的 tag
            true
        }
    }

    // 取回用于导航的 index
    private fun getIndex(id: Int) = when (id) {
        R.id.index_my -> myIndex
        R.id.index_search -> searchIndex
        R.id.index_listen -> listenIndex
        else -> throw Exception("using an invalid id!")
    }

    // 在 on resume & on pause 设置会导致闪烁
    override fun onStart() {
        super.onStart()
        mainViewModel.fragmentHost?.show() // 防止残留
    }

    override fun onStop() {
        mainViewModel.fragmentHost?.hide() // 防止残留
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainViewModel.fragmentHost?.removeOnIndexChangeListener()
        releaseMusicController()
    }

    fun onSongPass(playlist: List<Song>) {
        MusicController.resetPlaylist(playlist)
    }

    private fun startRemoteControl() { // 启动前台服务通知
        if (binder?.isBinderAlive == true) return
        appContext?.run {
            val i = Intent(this, RemoteControlService::class.java)
            this.bindService(i, RemoteControlConnection(), Context.BIND_AUTO_CREATE)
            this.startService(i)
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        handleBackPress()
    }

    /**
     * 返回逻辑
     *
     * @param enableTwoClickToExit 传入 false 时点击不双击退出
     */
    private fun handleBackPress(enableTwoClickToExit: Boolean = true) = mainViewModel.run {
        val fragment = fragmentHost!!.getCurrentFragment()
        FragmentTag.apply {
            when (fragment) {
                is TopPlaylistFragment ->
                    fragmentHost!!.pop(
                        TOP_PLAYLIST_FRAGMENT,
                        LISTEN_FRAGMENT,
                        R.anim.fade_in,
                        R.anim.right_exit
                    )

                is ResultFragment -> fragmentHost!!.navigate(
                    PREVIEW_FRAGMENT,
                    R.anim.fade_in,
                    R.anim.fade_out
                )

                is MyFragment -> dismissCallback?.dismissDialog() ?: {
                    if (enableTwoClickToExit) twoClicksToExit()
                }

                else -> if (enableTwoClickToExit) twoClicksToExit()
            }
        }
    }

    // 双击退出应用的逻辑
    override fun twoClicksToExit() {
        if (oneMoreClickToExit) {
            finishAffinity()
        } else {
            oneMoreClickToExit = true

            toast("再次点击退出")
            exitJob?.cancel()
            exitJob = lifecycleScope.launch {
                delay(2000)
                oneMoreClickToExit = false
            }
        }
    }

    /**
     * 用于记录索引
     */
    inner class OnIndexChangeListenerImpl : FragmentHost.OnIndexChangeListener {

        override fun onIndexChanged(index: Int) {
            FragmentTag.apply {
                when (index) {
                    LISTEN_FRAGMENT -> listenIndex = index
                    TOP_PLAYLIST_FRAGMENT -> {
                        listenIndex = index
                    }

                    PREVIEW_FRAGMENT -> searchIndex = index
                    RESULT_FRAGMENT -> {
                        searchIndex = index
                    }
                }
                previousTag = getNewTag(index)
            }
        }
    }

    // 旨在在各个播放器上显示准确的状态, 当 music controller 完成了工作才通知才更新通知
    inner class MusicControllerListenerImpl : MusicControllerListener {
        private var currentSong: Song? = null

        override fun onSwitchedToNext(song: Song) {
            binder?.changeSong(song)
            if (currentSong != song)
                setSong(song)
            currentSong = song
        }

        override fun onSwitchedToPrevious(song: Song) {
            binder?.changeSong(song)
            if (currentSong != song)
                setSong(song)
            currentSong = song
        }

        override fun onSongPaused() {
            binder?.setPlayingStatus(false)
            binding.play.switchImage(PlayButton.PLAY) // 暂停时显示 |> 的按钮
        }

        override fun onSongPlayed(song: Song) {
            binding.play.switchImage(PlayButton.PAUSE) // 显示 || 按钮
            binder?.changeSong(song)
            if (currentSong != song)
                setSong(song)
            binder?.setPlayingStatus(true)
            currentSong = song
        }

        override fun onProgressUpdated(newProgress: Int) {
            binding.seekBar.progress = newProgress
        }

        override fun onPlayModeChanged(newState: Int) {
            binding.playMode.run {
                when (newState) {
                    MusicController.LOOP -> switchImage(PlayModeButton.LOOP)
                    MusicController.SINGLE -> switchImage(PlayModeButton.SINGLE)
                    MusicController.RANDOM -> switchImage(PlayModeButton.RANDOM)
                }
            }
        }
    }

    /**
     * 用于通知播放器的 connection
     */
    inner class RemoteControlConnection : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            binder = service as RemoteControlService.RemoteControlBinder
        }

        override fun onServiceDisconnected(className: ComponentName) {
            binder = null
        }
    }

    inner class OnSeekBarChangeListenerImpl : SeekBar.OnSeekBarChangeListener {
        private var progress = 0

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser)
                this.progress = progress
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            MusicController.seekToPosition(progress)
        }
    }

    /**
     * 绑定播放器和导航栏(播放器展开时导航栏收缩)
     */
    inner class BottomSheetCallbackImpl : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
        }

        /**
         * [0, 1] 表示介于折叠和展开状态之间, [-1, 0] 介于隐藏和折叠状态之间, 此处由于禁止 hide 所以只会取值在[0, 1]
         *
         * 此处 slideOffset 完全可以当作一个百分数来看待
         */
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            val bottomNavigationHeight = binding.bottomNavigation.height.toFloat()

            var n = slideOffset * 2
            if (n > 1) n = 1F
            val translationY =
                bottomNavigationHeight * n
            binding.bottomNavigation.translationY = translationY

            if (slideOffset < 0.01) {
                binding.player.setBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.bar
                    )
                )
            } else {
                binding.player.background = playerBackground
            }
        }
    }

    /**
     * 设置 bottom sheet behavior 的歌曲
     */
    private fun setSong(song: Song) {
        binding.run {
            Glide.with(this@MainActivity)
                .load(song.al.picUrl)
                .override(100) // 先把原图质量变低再模糊就会是比较好的模糊效果
                .fitCenter()
                .transform(BlurTransformation(this@MainActivity, 40))
                .into(object : CustomTarget<Drawable?>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable?>?
                    ) {
                        playerBackground = resource
                        if (playerBehavior.state != BottomSheetBehavior.STATE_COLLAPSED) {
                            val transitionDrawable =
                                TransitionDrawable(arrayOf(player.background, resource))
                            player.background = transitionDrawable
                            transitionDrawable.startTransition(200)
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
            cover.setRadiusImgView(song.al.picUrl, radius = 40)
            songName.text = song.name
            singerName.formatDetails(song)
        }
    }

//    private fun setPlayerRadius(radius: Float) {
//        val shape = GradientDrawable().apply {
//            shape = GradientDrawable.RECTANGLE
//            setColor(Color.RED) // 设置背景颜色
//            cornerRadii = floatArrayOf(
//                radius, radius, // 左上角
//                radius, radius, // 右上角
//                0f, 0f,       // 右下角
//                0f, 0f        // 左下角
//            )
//        }
//
//        binding.player.background = shape
//    }

    // 部分数据存取逻辑
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // 包含 initBinding 的调用
        savedInstanceState?.run {
            listenIndex = getInt(LISTEN_KEY)
            myIndex = getInt(MY_KEY)
            searchIndex = getInt(SEARCH_KEY)

            currentTag = getInt(CURRENT)
            previousTag = getInt(PREVIOUS)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putInt(LISTEN_KEY, listenIndex)
            putInt(MY_KEY, myIndex)
            putInt(SEARCH_KEY, searchIndex)

            putInt(CURRENT, currentTag)
            putInt(PREVIOUS, previousTag)
        }
    }
}