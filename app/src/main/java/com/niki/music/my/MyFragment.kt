package com.niki.music.my

import android.os.Build
import android.view.View
import android.view.animation.AnimationUtils
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.niki.music.R
import com.niki.music.common.MusicRepository
import com.niki.music.common.commonViewModels.MusicViewModel
import com.niki.music.common.ui.SongAdapter
import com.niki.music.databinding.FragmentMyBinding
import com.niki.music.model.Song
import com.niki.music.my.login.LoginFragment
import com.niki.utils.ImageSetter
import com.niki.utils.base.BaseFragment
import com.niki.utils.base.logE
import com.niki.utils.base.ui.BaseLayoutManager
import com.niki.utils.takePartOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MyFragment : BaseFragment<FragmentMyBinding>() {

    companion object {
        const val CLICK_TO_LOGIN = "点击登录"
        const val NOT_YET_LOGGED_IN = "未登录"
        const val LOGOUT = "登出"
    }

    private lateinit var songAdapter: SongAdapter
    private lateinit var baseLayoutManager: BaseLayoutManager

    private val myViewModel: MyViewModel by activityViewModels<MyViewModel>()
    private val musicViewModel: MusicViewModel by activityViewModels<MusicViewModel>()

    override fun FragmentMyBinding.initBinding() {
        initValues()

        appbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            toolbar.visibility =
                if (abs(verticalOffset) == appBarLayout.totalScrollRange) View.VISIBLE else View.GONE
        }

        recyclerView.apply {
            isNestedScrollingEnabled = false
            adapter = songAdapter
            layoutManager = baseLayoutManager
            animation = AnimationUtils.loadAnimation(context, R.anim.fade_in_anim)
            addItemDecoration(
                DividerItemDecoration(
                    requireActivity(),
                    DividerItemDecoration.VERTICAL
                )
            )
        }

        handleStates()
    }

    private fun handleStates() {
        lifecycleScope.launch {
            myViewModel.uiEffectFlow
                .collect {
                    logE(TAG, "collected ${it::class.qualifiedName.toString()}")
                    when (it) {
                        is MyEffect.GetLikePlaylistEffect -> if (it.isSuccess)
                            songAdapter.submitPartly(MusicRepository.mLikePlaylist)

                        else -> {}
                    }
                }
        }
        myViewModel.apply {
            observeState {
                lifecycleScope.launch {
                    map { it.loggedInDatas }.distinctUntilChanged().collect {
                        if (it == null)
                            setUnLoggedInViews()
                        else
                            setLoggedInViews(it)
                    }
                }
            }
        }
    }

    private fun SongAdapter.submitPartly(list: List<Song>) = lifecycleScope.launch {
        val newList = takePartOf(list)
        if (newList.isNotEmpty())
            submitList(newList)
        else
            submitList(emptyList())
    }

    private fun setUnLoggedInViews() {
        mBinding.apply {
            userAvatar.visibility = View.INVISIBLE
            background.visibility = View.INVISIBLE
            nickname.text = NOT_YET_LOGGED_IN
            logout.text = CLICK_TO_LOGIN
            logout.setOnClickListener {
                LoginFragment().show(
                    requireActivity().supportFragmentManager,
                    LoginFragment::class.simpleName!!
                )
            }
        }
    }

    private fun setLoggedInViews(data: LoggedInDatas) {
        mBinding.run {
            nickname.text = data.nickname
            logout.text = LOGOUT
            logout.setOnClickListener {
                myViewModel.sendIntent(MyIntent.Logout)
            }

            ImageSetter.apply {
                userAvatar.setCircleImgView(
                    data.avatarUrl,
                    enableCache = true
                )
                background.setImgView(
                    data.backgroundUrl,
                    enableCache = true
                )
            }

            if (MusicRepository.mLikePlaylist.isEmpty())
                myViewModel.sendIntent(MyIntent.GetLikePlaylist)
            else
                songAdapter.submitPartly(MusicRepository.mLikePlaylist)
        }
    }

    private fun initValues() {
        songAdapter = SongAdapter(musicViewModel)
        baseLayoutManager = BaseLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, 4)
    }
}


//
//class CustomLayoutParams : ViewGroup.LayoutParams {
//    var gravity: Int = 0
//
//    constructor(c: Context, attrs: AttributeSet?) : super(c, attrs) {
//        val a = c.obtainStyledAttributes(attrs, R.styleable.CustomLayout)
//        gravity = a.getInteger(R.styleable.CustomLayout_gravity, 0)
//        a.recycle()
//    }
//
//    constructor(width: Int, height: Int) : super(width, height)
//
//    constructor(source: ViewGroup.LayoutParams?) : super(source)
//}
//
//class CustomLayout : ViewGroup {
//    constructor(context: Context?) : super(context)
//
//    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
//
//    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
//        context,
//        attrs,
//        defStyleAttr
//    )
//
//    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
//        val childCount = childCount
//        for (i in 0 until childCount) {
//            val child = getChildAt(i)
//            val layoutParams = child.layoutParams as CustomLayoutParams
//            val childWidth = child.measuredWidth
//            val childHeight = child.measuredHeight
//
//            var left = 0
//            var top = 0
//            when (layoutParams.gravity) {
//                0 -> {
//                    left = (r - l - childWidth) / 2
//                    top = (b - t - childHeight) / 2
//                }
//
//                1 -> {
//                    left = l
//                    top = (b - t - childHeight) / 2
//                }
//
//                2 -> {
//                    left = r - l - childWidth
//                    top = (b - t - childHeight) / 2
//                }
//            }
//            child.layout(left, top, left + childWidth, top + childHeight)
//        }
//    }
//
//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
//        val childCount = childCount
//        for (i in 0 until childCount) {
//            val child = getChildAt(i)
//            measureChild(child, widthMeasureSpec, heightMeasureSpec)
//        }
//    }
//
//    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
//        return CustomLayoutParams(context, attrs)
//    }
//
//    override fun generateDefaultLayoutParams(): LayoutParams {
//        return CustomLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
//    }
//
//    override fun checkLayoutParams(p: LayoutParams): Boolean {
//        return p is CustomLayoutParams
//    }
//}
//class FlowLayout(context: Context?, attrs: AttributeSet) :
//    ViewGroup(context, attrs) {
//    private var attributeSet: AttributeSet
//    private var colorSet: Int = Random().nextInt(4)
//    private var marginLayoutParams: MarginLayoutParams =
//        MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
//
//    init {
//        marginLayoutParams.setMargins(5, 5, 5, 5)
//        this.attributeSet = attrs
//    }
//
//    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
//        val maxWidth = r - l
//        var lineHeight = 0
//        var lineWidth = 0
//        var left = 0
//        var top = 0
//
//        for (i in 0 until childCount) {
//            val child = getChildAt(i)
//            val lp = child.layoutParams as MarginLayoutParams
//            val childWidth = child.measuredWidth + lp.leftMargin + lp.rightMargin
//            val childHeight = child.measuredHeight + lp.topMargin + lp.bottomMargin
//
//            // 判断是否需要换行
//            if (lineWidth + childWidth > maxWidth) {
//                top += lineHeight
//                lineHeight = childHeight
//                lineWidth = childWidth
//                left = 0
//            } else {
//                lineWidth += childWidth
//                lineHeight = max(lineHeight.toDouble(), childHeight.toDouble()).toInt()
//            }
//
//            val lc = left + lp.leftMargin
//            val tc = top + lp.topMargin
//            val rc = lc + child.measuredWidth
//            val bc = tc + child.measuredHeight
//            child.layout(lc, tc, rc, bc)
//
//            left += childWidth
//        }
//    }
//
//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        val maxWidth = MeasureSpec.getSize(widthMeasureSpec)
//        var totalHeight = 0
//        var lineWidth = 0
//        var lineHeight = 0
//
//        for (i in 0 until childCount) {
//            val child = getChildAt(i)
//            val lp = child.layoutParams as MarginLayoutParams
//            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
//            val childWidth = child.measuredWidth + lp.leftMargin + lp.rightMargin
//            val childHeight = child.measuredHeight + lp.topMargin + lp.bottomMargin
//
//            if (lineWidth + childWidth > maxWidth) {
//                totalHeight += lineHeight
//                lineHeight = childHeight
//                lineWidth = childWidth
//            } else {
//                lineWidth += childWidth
//                lineHeight = max(lineHeight.toDouble(), childHeight.toDouble()).toInt()
//            }
//        }
//
//        totalHeight += lineHeight
//        setMeasuredDimension(maxWidth, totalHeight)
//    }
//
//    fun addLabel(label: String?) {
//        val myTextView = MyTextView(context, attributeSet)
//        myTextView.setText(label)
//        myTextView.setLayoutParams(marginLayoutParams)
//        myTextView.setOnClickListener(this)
//        addView(myTextView)
//    }
//}
