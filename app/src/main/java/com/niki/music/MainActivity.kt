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
import com.niki.common.utils.Point
import com.niki.common.utils.getIntersectionPoint
import com.niki.common.utils.getNewTag
import com.niki.common.utils.setSongDetails
import com.niki.common.values.FragmentTag
import com.niki.music.databinding.ActivityMainBinding
import com.niki.music.listen.top.PlaylistFragment
import com.niki.music.my.MyFragment
import com.niki.music.my.login.dismissCallback
import com.niki.music.ui.BlurTransformation
import com.niki.music.ui.button.PlayButton
import com.niki.music.viewModel.MainViewModel
import com.p1ay1s.base.ActivityPreferences
import com.p1ay1s.base.appBaseUrl
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

// baseUrl 在 MusicApp

class MainActivity : ViewBindingActivity<ActivityMainBinding>(),
    ActivityPreferences.TwoClicksListener {

    companion object {
        const val LISTEN_KEY = "LISTEN"
        const val MY_KEY = "MY"
        const val SEARCH_KEY = "SEARCH"

        const val CURRENT = "CURRENT"
        const val PREVIOUS = "PREVIOUS"

        const val PROGRESS = "PROGRESS"

        const val BOTTOM_NAV_WEIGHT = 0.1
    }

    private var serviceBinder: MusicService.MusicServiceBinder? = null
    private var connection = MusicServiceConnection()

    private var exitJob: Job? = null

    private var oneMoreClickToExit = false

    private lateinit var mainViewModel: MainViewModel

    /**
     * 用于导航
     */
    private var listenIndex = FragmentTag.LISTEN_FRAGMENT
    private var myIndex = FragmentTag.MY_FRAGMENT
    private var searchIndex = FragmentTag.RESULT_FRAGMENT

    /**
     * 用于处理 bottom navigation view 的点击事件
     *
     * 比如点击同一个选项回退上一界面等
     */
    private var currentTag = FragmentTag.LISTEN_FRAGMENT
    private var previousTag = FragmentTag.LISTEN_FRAGMENT

    private var musicProgress = 0

    private val playerBehavior
        get() = BottomSheetBehavior.from(binding.player)

    override fun ActivityMainBinding.initBinding() {
        // 非得要 activity 的上下文
        appLoadingDialog = LoadingDialog(this@MainActivity)
        mainViewModel = ViewModelProvider(this@MainActivity)[MainViewModel::class.java]
        appFadeInAnim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_in)

        checkServerUsability()
        startMusicService()

        mainViewModel.run {
            if (fragmentHost == null) {
                fragmentHost =
                    fragmentHostView.create(supportFragmentManager, fragmentMap)
                fragmentHost?.setOnIndexChangeListener(OnIndexChangeListenerImpl())
            } else {
                fragmentHostView.restore(fragmentHost!!, supportFragmentManager)
                fragmentHost?.setOnIndexChangeListener(OnIndexChangeListenerImpl())
            }

            // 重建时恢复状态
            if (currentSong != null) {
                playerBehavior.isHideable = false
                playerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                setSong(currentSong!!)
            } else {
                playerBehavior.isHideable = true
                playerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }

            if (playerBackground == null)
                playerBackground = player.background
            if (playerBackground != null && playerBehavior.state != BottomSheetBehavior.STATE_COLLAPSED && playerBehavior.state != BottomSheetBehavior.STATE_HIDDEN)
                player.background = playerBackground
        }

        bottomNavigation.setSwitchHandler()

        playerBehavior.apply {
            addBottomSheetCallback(BottomSheetCallbackImpl())
            player.setOnClickListener {
                if (this.state != BottomSheetBehavior.STATE_EXPANDED)
                    this.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        seekBar.setOnSeekBarChangeListener(OnSeekBarChangeListenerImpl())

        play.setOnClickListener {
            serviceBinder?.play()
        }
        previous.setOnClickListener {
            serviceBinder?.previous()
        }
        next.setOnClickListener {
            serviceBinder?.next()
        }
        playMode.setOnClickListener {
            serviceBinder?.changePlayMode()
        }
        smallNext.setOnClickListener {
            serviceBinder?.next()
        }
        smallPlay.setOnClickListener {
            serviceBinder?.play()
        }

        setViewsLayoutParams()
    }

    /**
     * 不用约束布局只能用 layout params 设置
     */
    private fun setViewsLayoutParams() {
        binding.apply {
            val parentHeight = root.resources.displayMetrics.heightPixels
            val parentWidth = root.resources.displayMetrics.widthPixels
            val navHeight = (parentHeight * BOTTOM_NAV_WEIGHT).toInt()
            fragmentHostView.updateLayoutParams {
                height = parentHeight - navHeight
            }
            bottomNavigation.updateLayoutParams {
                height = navHeight
            }
            cover.updateLayoutParams {
                width = (0.85 * parentWidth).toInt()
                height = (0.85 * parentWidth).toInt()
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
    private fun startMusicService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            withPermission(POST_NOTIFICATIONS) {
                if (!it)
                    toast("未授予通知权限, 无法启用状态栏控制")
                else {
                    if (serviceBinder?.isBinderAlive == true) return@withPermission
                    val i = Intent(this, MusicService::class.java)
                    bindService(i, connection, Context.BIND_AUTO_CREATE)
                    startService(i)
                }
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
        serviceBinder?.setListener(null)
        unbindService(connection) // 不仅是要同一个 connection, 还得是同一个 context
    }

    fun onSongPass(playlist: List<Song>) {
        serviceBinder?.resetPlaylist(playlist)
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
                is PlaylistFragment ->
                    fragmentHost!!.pop(
                        TOP_PLAYLIST_FRAGMENT,
                        LISTEN_FRAGMENT,
                        R.anim.fade_in,
                        R.anim.right_exit
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
                    LISTEN_FRAGMENT, TOP_PLAYLIST_FRAGMENT -> {
                        listenIndex = index
                    }

                    RESULT_FRAGMENT -> {
                        searchIndex = index
                    }
                }
                previousTag = getNewTag(index)
            }
        }
    }

    // 旨在在各个播放器上显示准确的状态, 当 music controller 完成了工作才通知才更新通知
    inner class MusicControllerListenerImpl : MusicServiceListener {

        override fun onPlayingStateChanged(song: Song, isPlaying: Boolean) {
            if (mainViewModel.currentSong != song) {
                playerBehavior.run {
                    if (state == BottomSheetBehavior.STATE_HIDDEN) {
                        state = BottomSheetBehavior.STATE_COLLAPSED
                        lifecycleScope.launch { // 立即设置 false 会导致页面从顶端回落
                            while (true) {
                                if (state != BottomSheetBehavior.STATE_COLLAPSED) {
                                    delay(20)
                                } else {
                                    isHideable = false
                                    return@launch
                                }
                            }
                        }
                    }
                }
                setSong(song)
                mainViewModel.currentSong = song
            }

            binding.run {
                if (isPlaying) {
                    play.switchImage(PlayButton.PAUSE)
                    smallPlay.setImageResource(R.drawable.ic_pause)
                } else {
                    play.switchImage(PlayButton.PLAY)
                    smallPlay.setImageResource(R.drawable.ic_play)
                }
            }
        }

        override fun onProgressUpdated(newProgress: Int) {
            binding.seekBar.progress = newProgress
        }

        override fun onPlayModeChanged(newState: Int) {
            binding.playMode.switchImage(newState)
        }
    }

    /**
     * 用于通知播放器的 connection
     */
    inner class MusicServiceConnection : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            serviceBinder = service as MusicService.MusicServiceBinder
            serviceBinder?.setListener(MusicControllerListenerImpl())
        }

        override fun onServiceDisconnected(className: ComponentName) {
            serviceBinder = null
        }
    }

    inner class OnSeekBarChangeListenerImpl : SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser)
                musicProgress = progress
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            serviceBinder?.seekToPosition(musicProgress)
        }
    }


    /**
     * 绑定播放器和导航栏(播放器展开时导航栏收缩)
     */
    inner class BottomSheetCallbackImpl : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED) { // 弹出播放器后重设大小避免遮挡
                binding.apply {
                    val parentHeight = root.resources.displayMetrics.heightPixels
                    val navHeight = (parentHeight * BOTTOM_NAV_WEIGHT).toInt()
                    fragmentHostView.updateLayoutParams {
                        height = parentHeight - navHeight * 2
                    }
                }
            }
        }

        /**
         * [0, 1] 表示介于折叠和展开状态之间, [-1, 0] 介于隐藏和折叠状态之间, 此处由于禁止 hide 所以只会取值在[0, 1]
         *
         * 此处 slideOffset 完全可以当作一个百分数来看待
         */
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            val navHeight = binding.bottomNavigation.height.toFloat() // 导航栏高

            val navTranslationY = navHeight * slideOffset * 2 // 导航栏的偏移量
            if (slideOffset in 0.0F..1.0F)
                binding.bottomNavigation.translationY = navTranslationY

            bindCover(slideOffset)

            if (slideOffset < 0.005) { // 拉动一点点就隐藏歌名和设置背景
                binding.player.setBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.bar
                    )
                )
                binding.smallPlayer.visibility = View.VISIBLE
            } else {
                binding.player.background = mainViewModel.playerBackground
                binding.smallPlayer.visibility = View.INVISIBLE
            }
        }
    }

    /**
     * 用一个 [0, 1] 区间内的数设置 cover 的大小及位置
     *
     * pivot 计算原理: 以 cover 左上角为原点, 求完全缩小和原始尺寸的 cover 的右上角、左下角坐标的交点即为 pivot 点
     */
    private fun bindCover(slideOffset: Float) {
        val navHeight = binding.bottomNavigation.height.toFloat() // 导航栏高

        binding.cover.run {
            val coverHeight = height // 封面 imageview 宽高

            if (coverHeight == 0) return

            val minMargin = 0.1F * navHeight // 封面收缩后距离播放器的最小间距

            val minScale = (navHeight * 0.8F) / coverHeight // 使封面宽高到达最小的 scale 因子

            // (1 - t) * C + t * A 当 slide offset 约为 0 时结果为 minScale, 约为 1 时结果也为 1, 作用是限定 cover 的尺寸
            val scale = (1 - slideOffset) * minScale + slideOffset

            scaleX = scale
            scaleY = scale

            val pivotPoint = getIntersectionPoint( // 求完全展开和完全收缩的方形对应的点(用于计算 pivot)
                Point(minMargin + minScale * coverHeight, minMargin),
                Point(right.toFloat(), top.toFloat()),
                Point(minMargin, minMargin + minScale * coverHeight),
                Point(left.toFloat(), bottom.toFloat())
            ) // 此处得到的坐标并不是以 cover 的左上角为原点, 需要再计算

            pivotPoint?.let {
                val pivot = Point(pivotPoint.x - left, pivotPoint.y - top)
                pivotX = pivot.x
                pivotY = pivot.y
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
                        if (mainViewModel.currentSong != song) return

                        mainViewModel.playerBackground = resource
                        if (playerBehavior.state != BottomSheetBehavior.STATE_COLLAPSED && playerBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
                            val transitionDrawable =
                                TransitionDrawable(arrayOf(player.background, resource))
                            player.background = transitionDrawable
                            transitionDrawable.startTransition(600)
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
            cover.setRadiusImgView(song.al.picUrl, radius = 40)
            if (playerBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                bindCover(0F) // 让图片立即复位, 否则在初始化时不会显示在下端
            songName.text = song.name
            smallSongName.text = song.name
            singerName.setSongDetails(song)
        }
    }

    // 部分数据存取逻辑
    override fun onCreate(savedInstanceState: Bundle?) {
        savedInstanceState?.run {
            listenIndex = getInt(LISTEN_KEY)
            myIndex = getInt(MY_KEY)
            searchIndex = getInt(SEARCH_KEY)

            currentTag = getInt(CURRENT)
            previousTag = getInt(PREVIOUS)

            musicProgress = getInt(PROGRESS)
        }

        super.onCreate(savedInstanceState) // 包含 initBinding 的调用

//        val builder = MaterialAlertDialogBuilder(this)
//
//        builder.setTitle("material dialog test")
//            .setMessage("baseurl: http://\${your_ip}:3000/")
//            .setCancelable(true)
//            .setPositiveButton("positive") { dialog, which ->
//            }
//            .setNegativeButton("negative") { dialog, which ->
//            }
//
//        val dialog = builder.create()
//        dialog.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putInt(LISTEN_KEY, listenIndex)
            putInt(MY_KEY, myIndex)
            putInt(SEARCH_KEY, searchIndex)

            putInt(CURRENT, currentTag)
            putInt(PREVIOUS, previousTag)

            putInt(PROGRESS, musicProgress)
        }
    }
}