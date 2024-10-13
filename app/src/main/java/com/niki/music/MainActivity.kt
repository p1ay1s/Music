package com.niki.music

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.niki.common.ui.LoadingDialog
import com.niki.common.utils.getNewTag
import com.niki.common.values.FragmentTag
import com.niki.music.MusicController.registerMusicReceiver
import com.niki.music.MusicController.releaseMusicController
import com.niki.music.common.viewModels.MainViewModel
import com.niki.music.common.views.IView
import com.niki.music.databinding.ActivityMainBinding
import com.niki.music.intents.MainEffect
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
import com.p1ay1s.util.ServiceBuilder.ping
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

var appLoadingDialog: LoadingDialog? = null
var appFadeInAnim: Animation? = null

class MainActivity : ViewBindingActivity<ActivityMainBinding>(),
    ActivityPreferences.TwoClicksListener, FragmentHost.OnIndexChangeListener,
    MusicControllerListener, IView {

    companion object {
        const val LISTEN_KEY = "LISTEN"
        const val MY_KEY = "MY"
        const val SEARCH_KEY = "SEARCH"

        const val CURRENT = "current"
        const val PREVIOUS = "previous"
    }

    private var canPostNotification = false

    private var exitJob: Job? = null
    private var playMusicJob: Job? = null

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

    override fun ActivityMainBinding.initBinding() {
        // 非得要 activity 的上下文
        appLoadingDialog = LoadingDialog(this@MainActivity)
        MusicController.setListener(this@MainActivity)
        mainViewModel = ViewModelProvider(this@MainActivity)[MainViewModel::class.java]
        appFadeInAnim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_in)
        registerMusicReceiver()
        tryShowRemoteControl()

        handle()

        checkServerAbility()

        mainViewModel.run {
            if (fragmentHost == null) {
                fragmentHost =
                    fragmentHostView.create(supportFragmentManager, mainViewModel.fragmentMap)
                fragmentHost?.setOnIndexChangeListener(this@MainActivity)
            } else {
                fragmentHostView.restore(fragmentHost!!, supportFragmentManager)
                fragmentHost?.setOnIndexChangeListener(this@MainActivity)
            }
        }

        bottomNavigation.setSwitchHandler()
    }

    /**
     * 检查 localhost 是否可用
     */
    private fun checkServerAbility() {
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
        withPermission(POST_NOTIFICATIONS) {
            canPostNotification = it
            if (!it)
                toast("未授予通知权限, 无法启用状态栏控制")
            else
                startRemoteControl()
        }
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

    override fun onResume() {
        super.onResume()
        mainViewModel.fragmentHost?.show()// 防止残留
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

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        handleBackPress()
    }

    /**
     * 返回逻辑
     *
     * @param enableTwoClickToExit 点击 bottom nav 的时候不退出应用
     */
    private fun handleBackPress(enableTwoClickToExit: Boolean = true) {
        mainViewModel.run {
            val fragment = fragmentHost!!.getCurrentFragment()
            FragmentTag.apply {
                when (fragment) {
                    is TopPlaylistFragment -> {
                        fragmentHost!!.pop(
                            TOP_PLAYLIST_FRAGMENT,
                            LISTEN_FRAGMENT,
                            R.anim.fade_in,
                            R.anim.right_exit
                        )
                    }

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
    }

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
     * 记录索引
     */
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

    private fun startRemoteControl() { // 启动前台服务通知
        if (mainViewModel.binder?.isBinderAlive == true) return
        appContext?.run {
            val i = Intent(this, RemoteControlService::class.java)
            this.bindService(i, RemoteControlConnection(), Context.BIND_AUTO_CREATE)
            this.startService(i)
        }
    }

    override fun onSwitchToNext() {
        "->".toast()
    }

    override fun onSwitchToPrevious() {
        "<-".toast()
    }

    override fun onSongPaused() {
        // if success ...
        mainViewModel.binder?.setPlayingStatus(false)
    }

    override fun onSongPlayed() {
        // if success ...
        mainViewModel.binder?.setPlayingStatus(true)
    }

    /**
     * 处理 viewmodel 的事件
     */
    override fun handle() = lifecycleScope.apply {
        playMusicJob?.cancel()
        playMusicJob = launch {
            mainViewModel.uiEffectFlow.collect {
                if (it is MainEffect.TryPlaySongOkEffect) {
                    mainViewModel.binder?.changeSong(it.song) // 更新通知栏 remote views
                    MusicController.prepareSong(it.url)
                    MusicController.play()
                }
                if (it is MainEffect.TryPlaySongBadEffect) {
                    it.msg.toast()
                }
            }
        }
    }

    inner class RemoteControlConnection : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mainViewModel.binder = service as RemoteControlService.RemoteControlBinder
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mainViewModel.binder = null
        }
    }
}