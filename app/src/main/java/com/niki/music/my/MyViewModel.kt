package com.niki.music.my

import androidx.lifecycle.viewModelScope
import com.niki.common.repository.MusicRepository
import com.niki.common.repository.dataclasses.LoginResponse
import com.niki.common.utils.getStringData
import com.niki.common.utils.putStringData
import com.niki.common.values.preferenceAvatar
import com.niki.common.values.preferenceBackground
import com.niki.common.values.preferenceCookie
import com.niki.common.values.preferenceNickname
import com.niki.common.values.preferenceUid
import com.niki.music.common.viewModels.BaseViewModel
import com.niki.music.my.login.LoginModel
import com.p1ay1s.dev.base.TAG
import com.p1ay1s.dev.base.toast
import com.p1ay1s.dev.log.logE
import com.p1ay1s.dev.util.ON_FAILURE_CODE
import kotlinx.coroutines.launch

var appCookie = ""

class MyViewModel : BaseViewModel<MyIntent, MyState, MyEffect>() {
    private val loginModel by lazy { LoginModel() }

    override fun initUiState() = MyState(null, null, false, null)

    init {
        getLoginDatas {
            if (it != null) {
                updateState { copy(loggedInDatas = it, isLoggedIn = true) }
                checkCookieAbility()
            }
        }
    }

    override fun handleIntent(intent: MyIntent) {
        intent.run {
            logE(TAG, "RECEIVED " + this::class.simpleName.toString())
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

    private fun getAvatarUrl() = uiStateFlow.value.run {
        logE(TAG, phone.toString())
        if (phone.isNullOrBlank()) return@run

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
                    sendEffect { MyEffect.LoginFinish }
                },
                { code, _ ->
                    if (code != ON_FAILURE_CODE)
                        toast("验证码错误")
                    updateState { copy(isLoggedIn = false) }
                    sendEffect { MyEffect.LoginFinish }
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
     * login/refresh 接口获取的新 cookie 无论是否加密都是不可用的,
     * 我认为这是服务器的问题,
     * 因此这个接口只用于检测本地存储的 cookie 是否可用
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

    private fun getLikePlaylist() = uiStateFlow.value.loggedInDatas.run {
        if (this != null)
            playlistModel.getLikePlaylist(userId, cookie,
                {
                    getSongsWithIds(it.ids) { list ->
                        if (!list.isNullOrEmpty()) {
                            MusicRepository.likePlaylist = list
                            sendEffect { MyEffect.GetLikePlaylistEffect(true) }
                        } else
                            sendEffect { MyEffect.GetLikePlaylistEffect() }
                    }
                },
                { _, _ ->
                    sendEffect { MyEffect.GetLikePlaylistEffect() }
                })
    }

    private fun login(response: LoginResponse) {
        putLoggedInDatasPreference(response)
        updateLoggedInDatasState(response)
    }

    private fun logout(msg: String = "") = viewModelScope.launch {
        toast(msg)
        removeLoginDatas()
        setNotLoggedIn()
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