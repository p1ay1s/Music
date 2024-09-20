package com.niki.music

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import com.niki.base.view.BaseActivity
import com.niki.music.common.viewModels.MusicViewModel
import com.niki.music.databinding.ActivityMainBinding
import com.niki.music.listen.ListenFragment
import com.niki.music.my.MyFragment
import com.niki.music.my.MyViewModel
import com.niki.music.search.SearchFragment

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private lateinit var musicViewModel: MusicViewModel
    private lateinit var myViewModel: MyViewModel

    val index0 = "ListenFragment"
    val index1 = "MyFragment"
    val index2 = "SearchFragment"

    override fun ActivityMainBinding.initBinding() {
        val bottomNav = bottomNavMain
        fragmentControllerViewMain.run {
            setFragmentManager(supportFragmentManager)
            submitMap(
                linkedMapOf(
                    index0 to ListenFragment(),
                    index1 to MyFragment(),
                    index2 to SearchFragment()
                )
            )
            init()
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
    }
}