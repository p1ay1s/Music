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
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.ui.LoadingDialog
import com.niki.common.utils.Point
import com.niki.common.utils.formatDuration
import com.niki.common.utils.getIntersectionPoint
import com.niki.common.utils.getScreenHeight
import com.niki.common.utils.getScreenWidth
import com.niki.common.utils.getStringData
import com.niki.common.utils.isUrl
import com.niki.common.utils.putStringData
import com.niki.common.utils.restartApplication
import com.niki.common.utils.setMargins
import com.niki.common.utils.setSize
import com.niki.common.utils.setSongDetails
import com.niki.common.values.FragmentTag
import com.niki.common.values.preferenceBaseUrl
import com.niki.music.databinding.ActivityMainBinding
import com.niki.music.listen.ListenFragment
import com.niki.music.mine.MineFragment
import com.niki.music.search.ResultFragment
import com.niki.music.ui.BlurTransformation
import com.niki.music.ui.button.PlayButton
import com.niki.music.ui.loadCover
import com.niki.music.viewModel.MainViewModel
import com.p1ay1s.base.ActivityPreferences
import com.p1ay1s.base.appBaseUrl
import com.p1ay1s.base.appIpAddress
import com.p1ay1s.base.extension.toast
import com.p1ay1s.base.extension.withPermission
import com.p1ay1s.base.ui.FragmentHost
import com.p1ay1s.base.ui.FragmentHostView
import com.p1ay1s.impl.ViewBindingActivity
import com.p1ay1s.util.ServiceBuilder.ping
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

var appLoadingDialog: LoadingDialog? = null
var appFadeInAnim: Animation? = null
var appVibrator: Vibrator? = null

// baseUrl 在 MusicApp

class MainActivity : ViewBindingActivity<ActivityMainBinding>(),
    ActivityPreferences.TwoClicksListener {

    companion object {
        private const val SEEKBAR_SCALE = 3.0 // 进度条的细腻程度, 越大越细腻

        const val SEEKBAR_MAX = SEEKBAR_SCALE * 100

        const val BOTTOM_NAV_WEIGHT = 0.1
        const val MINI_PLAYER_WEIGHT = 0.075F

        const val MINI_COVER_SIZE = 0.8F // 占 mini player 高度的百分比
    }

    private var serviceBinder: MusicService.MusicServiceBinder? = null
    private var connection = MusicServiceConnection()

    private var exitJob: Job? = null
    private var backgroundJob: Job? = null

    private var oneMoreClickToExit = false

    private lateinit var mainViewModel: MainViewModel

    private var musicProgress = 0

    private val playerBehavior
        get() = BottomSheetBehavior.from(binding.player)

    private var parentHeight: Int = 0
    private var parentWidth: Int = 0
    private var bottomNavHeight: Int = 0
    private var miniPlayerHeight: Int = 0

    private val songLength: Int
        get() = serviceBinder?.getLength() ?: -1

    private var allowSetSeekbar = true

    override fun ActivityMainBinding.initBinding() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            enableEdgeToEdge()
            parentHeight = getScreenHeight()
            parentWidth = getScreenWidth()
        } else {
            parentHeight = binding.root.resources.displayMetrics.heightPixels
            parentWidth = binding.root.resources.displayMetrics.widthPixels
        }

        bottomNavHeight = (parentHeight * BOTTOM_NAV_WEIGHT).toInt()
        miniPlayerHeight = (parentHeight * MINI_PLAYER_WEIGHT).toInt()

        mainViewModel = ViewModelProvider(this@MainActivity)[MainViewModel::class.java]

        // 非得要 activity 的上下文
        appLoadingDialog = LoadingDialog(this@MainActivity)
        appFadeInAnim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_in)
        appVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        startMusicService()

        fragmentHostView.apply {
            this.fragmentManager = supportFragmentManager
            setOnHostChangeListener(OnHostChangeListenerImpl())
            if (mainViewModel.hostMap == null || mainViewModel.host == null) {
                addHost(R.id.index_search) {
                    pushFragment(
                        FragmentTag.RESULT_FRAGMENT,
                        ResultFragment::class.java
                    )
                }
                addHost(R.id.index_my) {
                    pushFragment(
                        FragmentTag.MY_FRAGMENT,
                        MineFragment::class.java
                    )
                }
                addHost(R.id.index_listen) {
                    pushFragment(
                        FragmentTag.LISTEN_FRAGMENT,
                        ListenFragment::class.java
                    )
                }
            } else {
                restore(mainViewModel.hostMap!!, mainViewModel.activityIndex)
            }
        }

        mainViewModel.run {
            // 重建时恢复状态
            if (currentSong != null) {
                playerBehavior.isHideable = false
                playerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                setSong(currentSong!!)
                serviceBinder?.getIsPlaying()?.let { setIsPlaying(it) }
            } else {
                playerBehavior.isHideable = true
                playerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }

            if (playerBackground == null)
                playerBackground = player.background
            if (playerBackground != null && playerBehavior.state != BottomSheetBehavior.STATE_COLLAPSED && playerBehavior.state != BottomSheetBehavior.STATE_HIDDEN)
                player.background = playerBackground
        }

        bottomNavigation.setListeners()

        playerBehavior.apply {
            addBottomSheetCallback(BottomSheetCallbackImpl())
            player.setOnClickListener {
                if (this.state != BottomSheetBehavior.STATE_EXPANDED)
                    this.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        seekBar.max = SEEKBAR_MAX.toInt()
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
            fragmentHostView.setSize(height = parentHeight - bottomNavHeight)
            bottomNavigation.setSize(height = bottomNavHeight)

            cover.setSize((0.85 * parentWidth).toInt())
            playlist.setSize((0.1 * parentWidth).toInt())
            playMode.setSize((0.1 * parentWidth).toInt())
            miniPlayer.setSize(
                height = miniPlayerHeight,
                width = parentWidth - miniPlayerHeight
            )
            play.setSize((0.17 * parentWidth).toInt())
            previous.setSize((0.16 * parentWidth).toInt())
            next.setSize((0.17 * parentWidth).toInt())

            cover.setMargins(top = (0.1 * parentHeight).toInt())
            songName.setMargins(top = (0.05 * parentHeight).toInt())
            line.setMargins(bottom = bottomNavHeight)

            playlist.setMargins(
                start = (0.03 * parentHeight).toInt(),
                bottom = (0.03 * parentHeight).toInt()
            )
            playMode.setMargins(
                end = (0.03 * parentHeight).toInt(),
                bottom = (0.03 * parentHeight).toInt()
            )

            playerBehavior.peekHeight = bottomNavHeight + miniPlayerHeight
        }
    }

    /**
     * 检查 localhost 是否可用
     */
    private suspend fun checkServerUsability() {
        appLoadingDialog?.show()
        while (!appBaseUrl.isUrl()) {
            delay(20)
        }
        ping(appBaseUrl) { isSuccess ->
            appLoadingDialog?.dismiss()
            if (!isSuccess) {
                requireNewUrl()
            } else {
                appBaseUrl.toast()
            }
        }
    }

    private fun requireNewUrl() {
        val builder = MaterialAlertDialogBuilder(this@MainActivity)

        val editText = EditText(this@MainActivity)
        editText.hint = "输入新的 baseurl"
        editText.setText(appBaseUrl)

        builder.setTitle("服务器连接失败")
            .setMessage("输入新的 baseurl")
            .setCancelable(true)
            .setView(editText)
            .setNegativeButton("提交") { _, _ ->
                var newUrl = editText.text.toString().trim()

                if (!newUrl.isUrl())
                    newUrl = appBaseUrl
                if (!newUrl.endsWith("/"))
                    newUrl += "/"

                lifecycleScope.launch {
                    putStringData(preferenceBaseUrl, newUrl)
                    restartApplication()
                }
            }
            .setNeutralButton("IP") { _, _ ->
                lifecycleScope.launch {
                    putStringData(preferenceBaseUrl, "http://$appIpAddress:3000/")
                    restartApplication()
                }
            }
            .setPositiveButton("取消") { _, _ ->
            }
        builder.create().show()
    }

    /**
     * 检查通知权限, 有权限则启动通知栏播放器
     */
    private fun startMusicService() {
        if (serviceBinder?.isBinderAlive == true) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withPermission(POST_NOTIFICATIONS) {
                if (serviceBinder?.isBinderAlive == true) return@withPermission
                if (!it)
                    toast("未授予通知权限, 无法启用状态栏控制")
                else {
                    val i = Intent(this, MusicService::class.java)
                    bindService(i, connection, Context.BIND_AUTO_CREATE)
                    startService(i)
                }
            }

            val i = Intent(this, MusicService::class.java)
            bindService(i, connection, Context.BIND_AUTO_CREATE)
            startService(i)
        } else {
            val i = Intent(this, MusicService::class.java)
            bindService(i, connection, Context.BIND_AUTO_CREATE)
            startService(i)
        }
    }

    /**
     * 设置导航的处理逻辑
     */
    private fun BottomNavigationView.setListeners() {
        setOnItemSelectedListener { item ->
            binding.fragmentHostView.switchHost(item.itemId, R.anim.fade_in, R.anim.fade_out)
            true
        }
        setOnItemReselectedListener {
            handleBackPress(false)
        }
    }

    // 在 on resume & on pause 设置会导致闪烁
    override fun onStart() {
        super.onStart()
        mainViewModel.host?.show() // 防止残留
    }

    override fun onStop() {
        mainViewModel.host?.hide() // 防止残留
        mainViewModel.hostMap = binding.fragmentHostView.map
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceBinder?.setListener(null)
        unbindService(connection) // 不仅是要同一个 connection, 还得是同一个 context
    }

    fun onSongPass(playlist: List<Song>) {
        serviceBinder?.updatePlaylist(playlist)
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
        val pair = mainViewModel.host?.getPeekPair()

        if (playerBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            playerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return@run
        }

        FragmentTag.run {
            if (pair != null)
                when (pair.first) {
                    LISTEN_FRAGMENT, MY_FRAGMENT, RESULT_FRAGMENT ->
                        if (enableTwoClickToExit) {
                            twoClicksToExit()
                        }

                    else -> mainViewModel.host?.popFragment(
                        R.anim.fade_in,
                        R.anim.right_exit
                    )
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

    private fun setIsPlaying(isPlaying: Boolean) {
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

    private fun setSeekbar(percent: Double, isReset: Boolean) {
        binding.seekBar.progress = (percent * SEEKBAR_MAX).toInt()
        setTimers(percent, isReset)
    }

    private fun setTimers(percent: Double, isReset: Boolean) = lifecycleScope.launch {
        val total: String
        val current: String

        val length = songLength
        if (length <= 0 || isReset) {
            total = "-"
            current = "0:00"
        } else {
            total = formatDuration(length)
            current = formatDuration((percent * length).toInt())
        }

        withContext(Dispatchers.Main) {
            binding.current.text = current
            binding.total.text = total
        }
    }

    inner class OnHostChangeListenerImpl : FragmentHostView.OnHostChangeListener {
        override fun onHostChanged(newHost: FragmentHost?, newIndex: Int) {
            newHost?.let {
                mainViewModel.host = it
                mainViewModel.activityIndex = newIndex
            }
        }
    }

    // 旨在在各个播放器上显示准确的状态, 当 music controller 完成了工作才通知才更新通知
    inner class MusicControllerListenerImpl : MusicServiceListener {

        override fun onSongChanged(song: Song) {
            if (mainViewModel.currentSong == song)
                return

            playerBehavior.run {
                if (state != BottomSheetBehavior.STATE_HIDDEN)
                    return@run
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

            mainViewModel.currentSong = song
            setSong(song)
        }

        override fun onPlayingStateChanged(isPlaying: Boolean) {
            setIsPlaying(isPlaying)
        }

        override fun onProgressUpdated(newProgress: Double, isReset: Boolean) {
            if (allowSetSeekbar)
                setSeekbar(newProgress, isReset)
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

        /**
         * progress max: 300
         */
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                musicProgress = progress
                setTimers(progress / SEEKBAR_MAX, false)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            allowSetSeekbar = false
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            setSeekbar(musicProgress / SEEKBAR_MAX, false)
            serviceBinder?.seekToPosition(musicProgress)
            allowSetSeekbar = true
        }
    }

    /**
     * 绑定播放器和导航栏(播放器展开时导航栏收缩)
     */
    inner class BottomSheetCallbackImpl : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED) { // 弹出播放器后重设大小避免遮挡
                binding.apply {
                    fragmentHostView.updateLayoutParams {
                        height = parentHeight - bottomNavHeight - miniPlayerHeight
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
            val navTranslationY = bottomNavHeight * slideOffset * 2 // 导航栏的偏移量
            if (slideOffset in 0.0F..1.0F) {
                binding.bottomNavigation.translationY = navTranslationY
                binding.line.translationY = navTranslationY
            }

            bindCover(slideOffset)

            if (slideOffset < 0.005) { // 拉动一点点就隐藏歌名和设置背景
                binding.player.setBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.bar
                    )
                )
                binding.miniPlayer.visibility = View.VISIBLE
            } else {
                binding.player.background = mainViewModel.playerBackground
                binding.miniPlayer.visibility = View.INVISIBLE
            }
        }
    }

    /**
     * 用一个 [0, 1] 区间内的数设置 cover 的大小及位置
     *
     * pivot 计算原理: 以 cover 左上角为原点, 求完全缩小和原始尺寸的 cover 的右上角、左下角坐标的交点即为 pivot 点
     */
    private fun bindCover(slideOffset: Float) {
        binding.cover.run {
            val coverHeight = height // 封面 imageview 宽高

            if (coverHeight == 0) return

            val minTopMargin = (1 - MINI_COVER_SIZE) / 2 * miniPlayerHeight  // 封面收缩后距离播放器的最小间距
            val minLeftMargin = 0.1F * miniPlayerHeight

            val minScale =
                (miniPlayerHeight * MINI_COVER_SIZE) / coverHeight // 使封面宽高到达最小的 scale 因子

            // (1 - t) * C + t * A 当 slide offset 约为 0 时结果为 minScale, 约为 1 时结果也为 1, 作用是限定 cover 的尺寸
            val scale = (1 - slideOffset) * minScale + slideOffset

            scaleX = scale
            scaleY = scale

            /*
            以 bottom sheet behavior 的左上角为参考系:

                a : 右上角, c : 左下角
                []

                    b : 右上角, d : 左下角
                    -------------
                    |           |
                    |   cover   |
                    |           |
                    |           |
                    -------------
             */
            val pivotPoint = getIntersectionPoint( // 求完全展开和完全收缩的方形对应的点(用于计算 pivot)
                Point(
                    minLeftMargin + minScale * coverHeight,
                    minTopMargin
                ), // a - coverHeight 其实是 coverWidth, 两者等大
                Point(right.toFloat(), top.toFloat()), // b
                Point(minLeftMargin, minTopMargin + minScale * coverHeight), // c
                Point(left.toFloat(), bottom.toFloat()) // d
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
                            backgroundJob?.cancel()
                            backgroundJob = lifecycleScope.launch(Dispatchers.Main) {
                                player.background = null
                                player.background = transitionDrawable
                                transitionDrawable.startTransition(600)
                            }
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })

            cover.loadCover(song.al.picUrl, radius = 40)
            if (playerBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                bindCover(0F) // 让图片立即复位, 否则在初始化时不会显示在下端
            songName.text = song.name
            smallSongName.text = song.name
            singerName.setSongDetails(song)
        }
    }

    // 部分数据存取逻辑
    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            val customUrl = getStringData(preferenceBaseUrl)
            val ipUrl = "http://$appIpAddress:3000/"
            appBaseUrl = customUrl.ifBlank { ipUrl }
            checkServerUsability()
        }

        super.onCreate(savedInstanceState) // 包含 initBinding 的调用
    }
}