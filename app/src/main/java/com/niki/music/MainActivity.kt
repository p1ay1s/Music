package com.niki.music


import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.niki.common.ui.LoadingDialog
import com.niki.common.values.FragmentTag
import com.niki.music.common.viewModels.MainViewModel
import com.niki.music.databinding.ActivityMainBinding
import com.niki.music.listen.ui.TopPlaylistFragment
import com.niki.music.my.MyFragment
import com.niki.music.my.MyViewModel
import com.niki.music.my.login.dismissCallback
import com.niki.music.search.result.ResultFragment
import com.p1ay1s.dev.base.ActivityPreferences
import com.p1ay1s.dev.base.appBaseUrl
import com.p1ay1s.dev.base.toast
import com.p1ay1s.dev.base.withPermission
import com.p1ay1s.dev.ui.FragmentHost
import com.p1ay1s.dev.util.IPSetter
import com.p1ay1s.dev.util.ServiceBuilder.ping
import com.p1ay1s.dev.viewbinding.ViewBindingActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

var appLoadingDialog: LoadingDialog? = null
var appFadeInAnim: Animation? = null

/**
 * 只让 OnFragmentIndexChangedListener 设置
 */


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainActivity : ViewBindingActivity<ActivityMainBinding>(),
    ActivityPreferences.TwoClicksListener, FragmentHost.OnFragmentIndexChangedListener {

    companion object {
        const val LISTEN_KEY = "LISTEN"
        const val MY_KEY = "MY"
        const val SEARCH_KEY = "SEARCH"
    }

    private var canPostNotification = false

    private var exitJob: Job? = null
    private var oneMoreClickToExit = false

    private lateinit var myViewModel: MyViewModel // 由于 loginFragment 和 myFragment 共享此 viewmodel
    private lateinit var mainViewModel: MainViewModel

    private var fragmentHost = mainViewModel.fragmentHost

    private var listenIndex = FragmentTag.LISTEN_FRAGMENT
    private var myIndex = FragmentTag.MY_FRAGMENT
    private var searchIndex = FragmentTag.PREVIEW_FRAGMENT

    private var fragmentTag = FragmentTag.LISTEN_FRAGMENT

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putInt(LISTEN_KEY, listenIndex)
            putInt(MY_KEY, myIndex)
            putInt(SEARCH_KEY, searchIndex)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // 包含 initBinding 的调用
        savedInstanceState?.run {
            listenIndex = getInt(LISTEN_KEY)
            myIndex = getInt(MY_KEY)
            searchIndex = getInt(SEARCH_KEY)
        }
    }

    override fun ActivityMainBinding.initBinding() {
        // 非得要 activity 的上下文
        appLoadingDialog = LoadingDialog(this@MainActivity)

        if (appFadeInAnim == null) appFadeInAnim =
            AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_in)

        IPSetter

        checkServerAbility()
        checkPermissions()

        initViewModels()

        if (fragmentHost == null) {
            fragmentHost =
                fragmentHostView.create(supportFragmentManager, mainViewModel.fragmentMap)
            fragmentHost?.setOnFragmentIndexChangeListener(this@MainActivity)
        } else {
            fragmentHostView.restore(fragmentHost!!, supportFragmentManager)
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

    private fun checkPermissions() {
        withPermission(POST_NOTIFICATIONS) {
            canPostNotification = it
            if (!it) toast("未授予通知权限, 无法启用状态栏控制")
        }
    }

    private fun initViewModels() {
        myViewModel = ViewModelProvider(this)[MyViewModel::class.java]
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }


    private fun BottomNavigationView.setSwitchHandler() {
        setOnItemSelectedListener { item ->
            val oldTag = fragmentTag
            fragmentTag = when (item.itemId) {
                R.id.myFragment -> myIndex
                R.id.previewFragment -> searchIndex
                else -> listenIndex // R.id.listenFragment
            }



            fragmentHost!!.navigate(
                fragmentTag,
                R.anim.fragment_enter_pop,
                R.anim.fragment_exit_pop,
            )
            true
        }
    }

    // 为避免混乱的情况最好在适当时机显示和隐藏
    override fun onResume() {
        super.onResume()
        fragmentHost!!.show()
    }

    // 为避免混乱的情况最好在适当时机显示和隐藏
    override fun onPause() {
        super.onPause()
        fragmentHost!!.hide()
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

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val fragment = fragmentHost!!.getCurrentFragment()
        FragmentTag.apply {
            when (fragment) {
                is TopPlaylistFragment -> {
                    fragmentHost!!.pop(
                        TOP_PLAYLIST_FRAGMENT,
                        LISTEN_FRAGMENT
                    )
                }

                is ResultFragment -> fragmentHost!!.navigate(PREVIEW_FRAGMENT)

                is MyFragment -> dismissCallback?.dismissDialog() ?: twoClicksToExit()

                else -> twoClicksToExit()
            }
        }
    }

    /**
     * 记录索引
     */
    override fun onFragmentIndexChanged(index: Int) {
        FragmentTag.apply {
            when (index) {
                LISTEN_FRAGMENT -> listenIndex = index
                TOP_PLAYLIST_FRAGMENT -> listenIndex = index
                PREVIEW_FRAGMENT -> searchIndex = index
                RESULT_FRAGMENT -> searchIndex = index
            }
        }
    }
}