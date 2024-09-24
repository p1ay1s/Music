package com.niki.music.search

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.niki.base.baseUrl
import com.niki.base.log.logE
import com.niki.base.view.BaseFragment
import com.niki.base.view.ui.BaseLayoutManager
import com.niki.music.common.ui.SongAdapter
import com.niki.music.common.viewModels.MusicViewModel
import com.niki.music.common.views.IView
import com.niki.music.databinding.FragmentSearchBinding
import com.niki.music.my.appCookie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

//interface ApiService {
//    @POST("eapi/ad/listening/rights/gain")
//    fun postRequest(
//        @Header("CMPagedId") cm: String,
//        @Header("X-MAM-CustomMark") xm: String,
//        @Header("Content-Type") contentType: String,
//        @Header("Connection") con: String,
//        @Header("Accept-Encoding") ac: String,
//        @Header("Cookie") cookie: String
//    ): Call<String>
//}


private val client = OkHttpClient()
//private val baseUrl = "http://interface3.music.163.com/"

fun post() {
    val resultBuffer = StringBuffer()
    try {
        val httpURLConnection =
            URL(baseUrl + "register/anonimous").openConnection() as HttpURLConnection
//        URL(baseUrl + "eapi/ad/listening/rights/gain").openConnection() as HttpURLConnection
        httpURLConnection.apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
//            setRequestProperty(
//                "User-Agent",
//                "NeteaseMusic/9.1.60.240919103339(9001060);Dalvik/2.1.0 (Linux; Ul Android; RMX3850 Build/UP1A.231005.007)"
//            )
//            setRequestProperty("CMPagedId", "AdMotivationVideoActivity")
//            setRequestProperty("X-MAM-CustomMark", "okhttp")
//            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
//            setRequestProperty("Connection", "Keep-Alive")
//            setRequestProperty("Accept-Encoding", "gzip")
//            setRequestProperty("Cookie", appCookie)
        }
        httpURLConnection.inputStream.use { inputStream ->
            InputStreamReader(inputStream, "utf-8").use { inputStreamReader ->
                BufferedReader(inputStreamReader).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        resultBuffer.append(line)
                    }
                    logE("AAAAA", resultBuffer.toString())
                }
            }
        }
    } finally {
        logE("AAAAA", "done")
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class SearchFragment : BaseFragment<FragmentSearchBinding>(), IView {
    private val searchViewModel: SearchViewModel by viewModels<SearchViewModel>()
    private val musicViewModel: MusicViewModel by activityViewModels<MusicViewModel>()

    private lateinit var songAdapter: SongAdapter
    private lateinit var baseLayoutManager: BaseLayoutManager

    override fun FragmentSearchBinding.initBinding() {
        initValues()

//        recyclerView.apply {
//            isNestedScrollingEnabled = false
//            adapter = songAdapter
//            layoutManager = baseLayoutManager
//            animation = AnimationUtils.loadAnimation(context, R.anim.fade_in_anim)
//            addItemDecoration(
//                DividerItemDecoration(
//                    requireActivity(),
//                    DividerItemDecoration.VERTICAL
//                )
//            )
//        }

        handle()

        lifecycleScope.launch(Dispatchers.IO) {
            post()
        }
    }

    override fun handle() = searchViewModel.apply {
        lifecycleScope.launch {
            uiEffectFlow
                .collect {
                    when (it) {
                        is SearchEffect.SearchSongsState -> {}
                    }
                }
        }
    }

    private fun initValues() {
        songAdapter = SongAdapter(musicViewModel)
        baseLayoutManager = BaseLayoutManager(
            requireActivity(),
            LinearLayoutManager.VERTICAL,
            4
        )
    }
}