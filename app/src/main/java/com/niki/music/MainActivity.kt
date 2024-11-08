package com.niki.music

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
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
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
import com.niki.music.ui.button.PlayButton
import com.niki.music.ui.loadBlurDrawable
import com.niki.music.ui.loadCCover
import com.niki.music.ui.loadDrawable
import com.niki.music.viewModel.MainViewModel
import com.p1ay1s.base.ActivityPreferences
import com.p1ay1s.base.appBaseUrl
import com.p1ay1s.base.appIpAddress
import com.p1ay1s.base.extension.toast
import com.p1ay1s.base.extension.withPermission
import com.p1ay1s.base.ui.FragmentHost
import com.p1ay1s.base.ui.FragmentHostView
import com.p1ay1s.util.ServiceBuilder.ping
import com.p1ay1s.vbclass.ViewBindingActivity
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

    private var oneMoreClickToExit = false

    private lateinit var mainViewModel: MainViewModel

    private var musicProgress = 0

    private val playerBehavior
        get() = BottomSheetBehavior.from(binding.player)

    private var _parentHeight: Int = 0
    val parentHeight: Int
        get() = _parentHeight
    private var _parentWidth: Int = 0
    val parentWidth: Int
        get() = _parentWidth
    private var bottomNavHeight: Int = 0
    private var miniPlayerHeight: Int = 0

    private val songDuration: Int
        get() = serviceBinder?.songDuration ?: -1

    private var allowSetSeekbar = true

    override fun ActivityMainBinding.initBinding() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            enableEdgeToEdge()
            _parentHeight = getScreenHeight()
            _parentWidth = getScreenWidth()
        } else {
            _parentHeight = binding.root.resources.displayMetrics.heightPixels
            _parentWidth = binding.root.resources.displayMetrics.widthPixels
        }

        bottomNavHeight = (_parentHeight * BOTTOM_NAV_WEIGHT).toInt()
        miniPlayerHeight = (_parentHeight * MINI_PLAYER_WEIGHT).toInt()

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
            setOnHostChangeListener(HostListenerImpl())
            if (mainViewModel.hostMap == null || mainViewModel.host == null) {
                addHost(R.id.index_search) { // 用 menu id 创建一个栈并添加一个 fragment
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
                serviceBinder?.isPlaying?.let { setIsPlaying(it) }
            } else {
                playerBehavior.isHideable = true
                playerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }

        bottomNavigation.setListeners()

        playerBehavior.apply {
            addBottomSheetCallback(SheetCallbackImpl())
            player.setOnClickListener {
                if (this.state != BottomSheetBehavior.STATE_EXPANDED)
                    this.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        seekBar.max = SEEKBAR_MAX.toInt()
        seekBar.setOnSeekBarChangeListener(SeekBarListenerImpl())

        play.setOnClickListener {
            serviceBinder?.switch()
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

        miniNext.setOnClickListener {
            serviceBinder?.next()
        }
        miniPlay.setOnClickListener {
            serviceBinder?.switch()
        }

        setViewsLayoutParams()
    }

    /**
     * 不用约束布局只能用 layout params 设置(由于使用 bottomSheetBehavior 需要 coordinatorLayout)
     */
    private fun setViewsLayoutParams() {
        binding.apply {
            fragmentHostView.setSize(height = _parentHeight - bottomNavHeight)
            bottomNavigation.setSize(height = bottomNavHeight)

            cover.setSize((0.85 * _parentWidth).toInt())
            playlist.setSize((0.1 * _parentWidth).toInt())
            playMode.setSize((0.1 * _parentWidth).toInt())

            miniPlayer.setSize(
                height = miniPlayerHeight,
                width = _parentWidth - miniPlayerHeight
            )
            play.setSize((0.17 * _parentWidth).toInt())
            previous.setSize((0.16 * _parentWidth).toInt())
            next.setSize((0.17 * _parentWidth).toInt())

            cover.setMargins(top = (0.1 * _parentHeight).toInt())
            songName.setMargins(top = (0.05 * _parentHeight).toInt())
            line.setMargins(bottom = bottomNavHeight)

            playlist.setMargins(
                start = (0.03 * _parentHeight).toInt(),
                bottom = (0.03 * _parentHeight).toInt()
            )
            playMode.setMargins(
                end = (0.03 * _parentHeight).toInt(),
                bottom = (0.03 * _parentHeight).toInt()
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

    /**
     * 当 ping 不通 baseurl 时显示 dialog要求输入新的 baseurl
     */
    private fun requireNewUrl() {
        val editText = EditText(this).also {
            it.hint = "输入新的 baseurl"
            it.setText(appBaseUrl)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("服务器连接失败")
            .setMessage("输入新的 baseurl")
            .setCancelable(true)
            .setView(editText)
            .setNegativeButton("提交") { _, _ ->
                var newUrl = editText.text.toString().trim()
                if (!newUrl.isUrl()) newUrl = appBaseUrl
                if (!newUrl.endsWith("/")) newUrl += "/"

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
            }.create().show()
    }

    /**
     * 检查通知权限, 有权限则启动通知栏播放器
     */
    private fun startMusicService() {
        if (serviceBinder?.isBinderAlive == true) return
        val i = Intent(this, MusicService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withPermission(POST_NOTIFICATIONS) {
                bindService(i, connection, Context.BIND_AUTO_CREATE)
                startService(i)
                if (!it)
                    toast("未授予通知权限, 无法启用状态栏控制")
            }
        } else {
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
        super.onStop() // on stop 之后不能再访问 fragment manager
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceBinder?.setMusicServiceListener(null)
        unbindService(connection) // 不仅是要同一个 connection, 还得是同一个 context
    }

    fun onSongPass(playlist: List<Song>) {
        serviceBinder?.setNewPlaylist(playlist)
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
                miniPlay.setImageResource(R.drawable.ic_pause)
            } else {
                play.switchImage(PlayButton.PLAY)
                miniPlay.setImageResource(R.drawable.ic_play)
            }
        }
    }

    private fun setSeekbar(percent: Double, isReset: Boolean) {
        setTimers(percent, isReset)
        binding.seekBar.progress = (percent * SEEKBAR_MAX).toInt()
    }

    private fun setTimers(percent: Double, isReset: Boolean) = lifecycleScope.launch {
        val total: String
        val current: String

        val length = songDuration
        if (length <= 0 || isReset) {
            binding.seekBar.setLoading(true)
            total = "-0:00"
            current = "0:00"
        } else {
            binding.seekBar.setLoading(false)
            total = formatDuration(length)
            current = formatDuration((percent * length).toInt())
        }

        withContext(Dispatchers.Main) {
            binding.current.text = current
            binding.total.text = total
        }
    }

    /**
     * 监听 fragment host 的当前栈变化
     */
    inner class HostListenerImpl : FragmentHostView.OnHostChangeListener {
        override fun onHostChanged(newHost: FragmentHost?, newIndex: Int) {
            newHost?.let {
                mainViewModel.host = it
                mainViewModel.activityIndex = newIndex
            }
        }
    }

    /**
     * 音乐服务的回调
     */
    inner class MusicServiceListenerImpl : MusicServiceListener {

        override fun onSongChanged(song: Song) {
            if (mainViewModel.currentSong == song) return

            playerBehavior.run {
                if (state != BottomSheetBehavior.STATE_HIDDEN) return@run

                state = BottomSheetBehavior.STATE_COLLAPSED
                lifecycleScope.launch { // 立即设置 false 会导致页面从顶端回落
                    while (true) {
                        if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                            isHideable = false
                            return@launch
                        }
                        delay(20)
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
            if (allowSetSeekbar) setSeekbar(newProgress, isReset) // 在用户拉动时不允许回调函数更新 seekbar
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
            serviceBinder?.setMusicServiceListener(MusicServiceListenerImpl())
        }

        override fun onServiceDisconnected(className: ComponentName) {
            serviceBinder = null
        }
    }

    inner class SeekBarListenerImpl : SeekBar.OnSeekBarChangeListener {

        /**
         * progress max: 300
         */
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (!fromUser) return
            musicProgress = progress
            setTimers(progress / SEEKBAR_MAX, false)
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
    inner class SheetCallbackImpl : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            binding.apply {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    fragmentHostView.updateLayoutParams { // 弹出播放器后重设大小避免遮挡
                        height = _parentHeight - bottomNavHeight - miniPlayerHeight
                    }

                    miniPlayer.visibility = View.VISIBLE
                } else {
                    miniPlayer.visibility = View.INVISIBLE
                }
            }
        }

        /**
         * [0, 1] 表示介于折叠和展开状态之间, [-1, 0] 介于隐藏和折叠状态之间, 此处由于禁止 hide 所以只会取值在[0, 1]
         *
         * 此处 slideOffset 完全可以当作一个百分数来看待
         */
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            if (slideOffset in 0.0F..1.0F) {
                binding.shade.alpha = 1 - slideOffset * 15 // 使小播放器渐变消失
                val navTranslationY = bottomNavHeight * slideOffset * 2 // 导航栏的偏移量
                binding.bottomNavigation.translationY = navTranslationY
                binding.line.translationY = navTranslationY
            }

            bindCover(slideOffset)
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
            loadDrawable(song.al.picUrl, true) { drawable ->
                cover.loadCCover(drawable, 40)
                loadBlurDrawable(drawable, 40) {
                    if (mainViewModel.currentSong != song) return@loadBlurDrawable

                    val transitionDrawable =
                        TransitionDrawable(
                            arrayOf(player.background, it)
                        )
                    player.background = null
                    player.background = transitionDrawable
                    transitionDrawable.startTransition(600)
                }
            }

            if (playerBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                bindCover(0F) // 让图片立即复位, 否则在初始化时不会显示在下端
            songName.text = song.name
            miniSongName.text = song.name
            singerName.setSongDetails(song)
        }
    }

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