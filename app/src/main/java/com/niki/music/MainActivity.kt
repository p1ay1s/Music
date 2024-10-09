package com.niki.music


import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.niki.common.ui.LoadingDialog
import com.niki.common.values.FragmentTag
import com.niki.music.browse.TopPlaylistFragment
import com.niki.music.common.viewModels.MusicViewModel
import com.niki.music.databinding.ActivityMainBinding
import com.niki.music.listen.ListenFragment
import com.niki.music.my.MyFragment
import com.niki.music.my.MyViewModel
import com.niki.music.my.login.dismissCallback
import com.niki.music.search.preview.PreviewFragment
import com.niki.music.search.result.ResultFragment
import com.p1ay1s.dev.base.ActivityPreferences
import com.p1ay1s.dev.base.appBaseUrl
import com.p1ay1s.dev.base.toast
import com.p1ay1s.dev.base.withPermission
import com.p1ay1s.dev.ui.FragmentHost
import com.p1ay1s.dev.util.ServiceBuilder.ping
import com.p1ay1s.dev.viewbinding.ViewBindingActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

var appLoadingDialog: LoadingDialog? = null
var appMusicViewModel: MusicViewModel? = null
var appFragmentHost: FragmentHost? = null

var listenIndex = FragmentTag.LISTEN_FRAGMENT
var searchIndex = FragmentTag.PREVIEW_FRAGMENT

val map: LinkedHashMap<String, Fragment> by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        linkedMapOf(
            FragmentTag.LISTEN_FRAGMENT to ListenFragment(),
            FragmentTag.MY_FRAGMENT to MyFragment(),
            FragmentTag.PREVIEW_FRAGMENT to PreviewFragment()
        )
    } else {
        throw Exception("unsupported sdk")
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainActivity : ViewBindingActivity<ActivityMainBinding>(),
    ActivityPreferences.TwoClicksListener {

    private val fragmentHost: FragmentHost
        get() = binding.fragmentHostViewMain.fragmentHost

    private var backPressTimer: Job? = null
    private var oneMoreToFinish = false

    private lateinit var myViewModel: MyViewModel

    override fun ActivityMainBinding.initBinding() {
        appLoadingDialog = LoadingDialog(this@MainActivity)

        appLoadingDialog?.show()

        ping(appBaseUrl) { isSuccess ->
            if (!isSuccess)
                toast("服务器连接失败")
            appLoadingDialog?.dismiss()
        }

        withPermission(POST_NOTIFICATIONS) {
            toast(if (it) "已授权" else "未授权")
        }

        myViewModel = ViewModelProvider(this@MainActivity)[MyViewModel::class.java]
        appMusicViewModel = ViewModelProvider(this@MainActivity)[MusicViewModel::class.java]

        appMusicViewModel!!.run {

            if (appFragmentHost == null)
                appFragmentHost = fragmentHostViewMain.create(
                    supportFragmentManager, map
                )
            else {
                fragmentHostViewMain.restore(appFragmentHost!!, supportFragmentManager)
                fragmentHost.addAll()
            }
        }

        bottomNavMain.setOnItemSelectedListener { item ->
            val tag = when (item.itemId) {
                R.id.listenFragment -> listenIndex

                R.id.myFragment -> FragmentTag.MY_FRAGMENT

                R.id.previewFragment -> searchIndex

                else -> FragmentTag.LISTEN_FRAGMENT
            }
            fragmentHost.navigate(tag)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        fragmentHost.show() // 必须在此处调用否则启动后不显示
    }

    override fun onPause() {
        super.onPause()
        fragmentHost.hide()
    }

    override fun twoClicksToExit() {
        if (oneMoreToFinish) {
            finishAffinity()
        } else {
            oneMoreToFinish = true

            toast("再次点击退出")
            backPressTimer?.cancel()
            backPressTimer = lifecycleScope.launch {
                delay(2000)
                oneMoreToFinish = false
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val fragment = fragmentHost.getCurrentFragment()
        when (fragment) {
            is TopPlaylistFragment -> {
                listenIndex = FragmentTag.LISTEN_FRAGMENT
                fragmentHost.pop(FragmentTag.TOP_PLAYLIST_FRAGMENT)
            }

            is ResultFragment -> {
                searchIndex = FragmentTag.PREVIEW_FRAGMENT
                fragmentHost.navigate(FragmentTag.PREVIEW_FRAGMENT)
            }

            is MyFragment -> {
                dismissCallback?.let {
                    it.dismissDialog()
                    return
                }
                twoClicksToExit()
            }

            else -> twoClicksToExit()
        }
    }
}