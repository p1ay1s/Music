package com.niki.music

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import com.niki.music.common.commonViewModels.MusicViewModel
import com.niki.music.databinding.ActivityMainBinding
import com.niki.music.listen.ListenFragment
import com.niki.music.my.MyFragment
import com.niki.music.my.MyViewModel
import com.niki.music.search.SearchFragment
import com.niki.utils.base.BaseActivity
import com.niki.utils.base.utils.BaseFragmentManagerHelper

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private lateinit var musicViewModel: MusicViewModel
    private lateinit var myViewModel: MyViewModel

    override fun ActivityMainBinding.initBinding() {
        val bottomNav = bottomNavMain
        val helper = BaseFragmentManagerHelper(
            supportFragmentManager,
            R.id.frameLayout_Main,
            listOf(
                ListenFragment(), MyFragment(), SearchFragment()
            )
        )
        bottomNav.setOnItemSelectedListener { item ->
            helper.switchToFragment(
                when (item.itemId) {
                    R.id.menu_listen -> 0
                    R.id.menu_my -> 1
                    R.id.menu_search -> 2
                    else -> 0
                }
            )
            true
        }

        musicViewModel = ViewModelProvider(this@MainActivity)[MusicViewModel::class.java]
        myViewModel = ViewModelProvider(this@MainActivity)[MyViewModel::class.java]
    }
}