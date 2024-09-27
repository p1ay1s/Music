package com.niki.music

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.niki.music.common.ui.LoadingDialog
import com.niki.music.common.viewModels.MusicViewModel
import com.niki.music.databinding.ActivityMainBinding
import com.niki.music.listen.ListenFragment
import com.niki.music.my.MyFragment
import com.niki.music.my.MyViewModel
import com.niki.music.search.SEARCH_PREVIEW
import com.niki.music.search.SearchFragment
import com.niki.music.search.result.SearchResultFragment
import com.p1ay1s.dev.base.ActivityPreferences
import com.p1ay1s.dev.base.appBaseUrl
import com.p1ay1s.dev.base.toast
import com.p1ay1s.dev.ui.FragmentControllerView
import com.p1ay1s.dev.util.ServiceBuilder.ping
import com.p1ay1s.dev.viewbinding.ViewBindingActivity
import com.p1ay1s.extensions.views.ContainerFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

var loadingDialog: LoadingDialog? = null
var onBackPressListener: OnBackPressListener? = null

interface OnBackPressListener {
    fun onBackPress()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainActivity : ViewBindingActivity<ActivityMainBinding>(), OnBackPressListener,
    FragmentControllerView.OnFragmentIndexChangedListener,
    ActivityPreferences.TwoBackPressToExitListener {

    private var backPressTimer: Job? = null
    private var oneMoreToFinish = false

    private lateinit var musicViewModel: MusicViewModel
    private lateinit var myViewModel: MyViewModel

    val index0 = "ListenFragment"
    val index1 = "MyFragment"
    val index2 = "SearchFragment"

    private lateinit var map: LinkedHashMap<String, Fragment>

    private var currentIndex = index0
    private lateinit var currentChildIndex: String

    override fun ActivityMainBinding.initBinding() {
        val bottomNav = bottomNavMain
        loadingDialog = LoadingDialog(this@MainActivity)
        map = linkedMapOf(
            index0 to ListenFragment(),
            index1 to MyFragment(),
            index2 to SearchFragment()
        )

        fragmentControllerViewMain.run {
            init(supportFragmentManager, map)
            setOnFragmentIndexChangeListener(this@MainActivity)
        }

        bottomNav.setOnItemSelectedListener { item ->
            fragmentControllerViewMain.switchToFragment(
                when (item.itemId) {
                    R.id.menu_my -> index1
                    R.id.menu_search -> index2
                    else -> index0
                }
            )
            true
        }

        musicViewModel = ViewModelProvider(this@MainActivity)[MusicViewModel::class.java]
        myViewModel = ViewModelProvider(this@MainActivity)[MyViewModel::class.java]

        loadingDialog?.show()
        ping(appBaseUrl) { isSuccess ->
            if (!isSuccess)
                toast("服务器连接失败")
            loadingDialog?.hide()
        }
    }

    override fun onBackPress() {
        runCatching {
            val containerFragment = (map[currentIndex] as ContainerFragment)
            containerFragment.controllerView?.run {
                when (getCurrentFragment()) {
                    is SearchResultFragment ->
                        this.switchToFragment(SEARCH_PREVIEW)

                    else -> twoBackPressToExit()
                }
            }
        }
    }

    override fun onFragmentIndexChanged(index: String) {
        currentIndex = index
    }

    override fun twoBackPressToExit() {
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
        onBackPress()
    }
}