package com.niki.music.my

import androidx.lifecycle.viewModelScope
import com.niki.music.common.MusicRepository
import com.niki.music.common.commonViewModels.BaseViewModel
import com.niki.music.model.LoginResponse
import com.niki.music.my.login.LoginModel
import com.niki.utils.TAG
import com.niki.utils.base.logE
import com.niki.utils.datastore.getStringData
import com.niki.utils.datastore.preferenceAvatar
import com.niki.utils.datastore.preferenceBackground
import com.niki.utils.datastore.preferenceCookie
import com.niki.utils.datastore.preferenceNickname
import com.niki.utils.datastore.preferenceUid
import com.niki.utils.datastore.putStringData
import com.niki.utils.toast
import com.niki.utils.webs.ON_FAILURE_CODE
import kotlinx.coroutines.launch

var appCookie = ""

class MyViewModel : BaseViewModel<MyIntent, MyState, MyEffect>() {
    private val loginModel by lazy { LoginModel() }

    override fun initUiState() = MyState(null, null, false, null)

    init {
        getLoginDatas {
            updateState { copy(loggedInDatas = it, isLoggedIn = it != null) }
            checkCookieAbility()
        }
    }

    override fun handleIntent(intent: MyIntent) {
        intent.run {
            when (this) {
                is MyIntent.UpdatePhone -> updateState { copy(phone = this@run.phone) }
                is MyIntent.UpdateCaptcha -> updateState { copy(captcha = this@run.captcha) }
                MyIntent.GetAvatarUrl -> getAvatarUrl()
                MyIntent.CaptchaLogin -> captchaLogin()
                MyIntent.GetLikePlaylist -> getLikePlaylist()
                MyIntent.SendCaptcha -> sendCaptcha()
                MyIntent.Logout -> logout()
            }
        }
    }

    private fun getAvatarUrl() {
        uiStateFlow.value.run {
            logE(TAG, phone.toString())
            if (phone.isNullOrBlank()) return

            loginModel.getAvatarUrl(phone,
                {
                    if (it.exist == 1)
                        sendEffect { MyEffect.GetAvatarUrlOkEffect(it.avatarUrl) }
                    else
                        sendEffect { MyEffect.GetAvatarUrlBadEffect }
                },
                { _, _ ->
                    sendEffect { MyEffect.GetAvatarUrlBadEffect }
                })
        }
    }

    private fun captchaLogin() = uiStateFlow.value.run {
        if (phone.isNullOrBlank() || captcha.isNullOrBlank()) {
            updateState { copy(isLoggedIn = false) }
            toast("请检查输入")
        } else
            loginModel.captchaLogin(phone, captcha,
                {
                    it.run {
                        if (code == 200) {
                            login(it)
                            updateState { copy(isLoggedIn = true) }
                            toast("欢迎回来! ${profile.nickname}")
                        } else {
                            updateState { copy(isLoggedIn = false) }
                            toast("验证码错误")
                        }
                    }
                },
                { code, _ ->
                    if (code != ON_FAILURE_CODE)
                        toast("验证码错误")
                    updateState { copy(isLoggedIn = false) }
                })
    }

    private fun sendCaptcha() = uiStateFlow.value.run {
        if (phone.isNullOrBlank()) {
            toast("请检查输入")
        } else
            loginModel.sendCaptcha(phone,
                {
                    toast("已发送")
                },
                { _, _ ->
                    toast("发送失败")
                })
    }

    /**
     * login/refresh 接口获取的新 cookie 无论是否加密都是不可用的, 我认为这是服务器的问题, 因此这个接口只用于检测本地存储的 cookie 是否可用
     */
    private fun checkCookieAbility() = uiStateFlow.value.run {
        if (loggedInDatas != null)
            loginModel.loginRefresh(loggedInDatas.cookie,
                {
                    if (it.code != 200)
                        logout("身份验证失败! 请重新登录")
                },
                { code, _ ->
                    if (code != ON_FAILURE_CODE)
                        logout()
                })
    }

    private fun getLikePlaylist() = viewModelScope.launch {
        uiStateFlow.value.loggedInDatas.run {
            if (this != null)
                playlistModel.getLikePlaylist(userId, cookie,
                    {
                        getSongsWithIds(it.ids) { list ->
                            if (!list.isNullOrEmpty()) {
                                MusicRepository.mLikePlaylist = list
                                sendEffect { MyEffect.GetLikePlaylistEffect(true) }
                            } else
                                sendEffect { MyEffect.GetLikePlaylistEffect() }
                        }
                    },
                    { _, _ ->
                        sendEffect { MyEffect.GetLikePlaylistEffect() }
                    })
        }
    }

    private fun login(response: LoginResponse) {
        putLoggedInDatasPreference(response)
        updateLoggedInDatasState(response)
    }

    private fun logout(msg: String = "") = viewModelScope.launch {
        toast(msg)
        removeLoginDatas()
        setNotLoggedIn()
        MusicRepository.mLikePlaylist = mutableListOf()
    }

    private inline fun getLoginDatas(crossinline callback: (LoggedInDatas?) -> Unit) =
        viewModelScope.launch {
            val uid = getStringData(preferenceUid)
            val nickname = getStringData(preferenceNickname)
            val cookie = getStringData(preferenceCookie)
            val avatarUrl = getStringData(preferenceAvatar)
            val backgroundUrl = getStringData(preferenceBackground)

            logE(TAG, if (cookie.isBlank()) "未登录" else "已经登录")
            appCookie = cookie

            when (cookie) {
                "" -> callback(null)
                else -> callback(
                    LoggedInDatas(
                        uid,
                        nickname,
                        cookie,
                        avatarUrl,
                        backgroundUrl
                    )
                )
            }
        }

    private fun putCookiePreference(cookie: String) = viewModelScope.launch {
        putStringData(preferenceCookie, cookie)
    }

    private fun putLoggedInDatasPreference(response: LoginResponse) = viewModelScope.launch {
        response.apply {
            putStringData(preferenceUid, profile.userId)
            putStringData(preferenceAvatar, profile.avatarUrl)
            putStringData(preferenceBackground, profile.backgroundUrl)
            putStringData(preferenceNickname, profile.nickname)
            putStringData(preferenceCookie, cookie)
        }
    }

    private fun updateLoggedInDatasState(response: LoginResponse) {
        response.run {
            updateState {
                copy(
                    loggedInDatas = LoggedInDatas(
                        profile.userId,
                        profile.nickname,
                        cookie,
                        profile.avatarUrl,
                        profile.backgroundUrl
                    )
                )
            }
        }
    }

    private fun updateLoggedInDatasState(
        userId: String,
        nickname: String,
        cookie: String,
        avatarUrl: String,
        backgroundUrl: String
    ) {
        updateState {
            copy(
                loggedInDatas = LoggedInDatas(
                    userId,
                    nickname,
                    cookie,
                    avatarUrl,
                    backgroundUrl
                )
            )
        }
    }

    private fun removeLoginDatas() = viewModelScope.launch {
        putStringData(preferenceUid, "")
        putStringData(preferenceAvatar, "")
        putStringData(preferenceBackground, "")
        putStringData(preferenceNickname, "")
        putStringData(preferenceCookie, "")
    }

    private fun setNotLoggedIn() =
        updateState { copy(loggedInDatas = null) }
}