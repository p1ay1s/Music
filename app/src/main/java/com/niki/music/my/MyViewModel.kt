package com.niki.music.my

import androidx.lifecycle.viewModelScope
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
import com.p1ay1s.base.extension.TAG
import com.p1ay1s.base.extension.toast
import com.p1ay1s.base.extension.toastSuspended
import com.p1ay1s.base.log.logE
import com.p1ay1s.util.ON_FAILURE_CODE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

var appCookie = ""

class MyViewModel : BaseViewModel<MyIntent, MyState, MyEffect>() {
    private val loginModel by lazy { LoginModel() }

    private var avatarJob: Job? = null

    override fun initUiState() = MyState(null, null, false, null, null)

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

    // Effect-only
    private fun getAvatarUrl() = uiStateFlow.value.run {
        if (phone.isNullOrBlank()) return@run

        viewModelScope.launch {
            avatarJob?.cancel()
            avatarJob?.join()
            avatarJob = launch(Dispatchers.IO) {
                delay(300) // 冷静期, 避免在某些情况下前一个请求反而后回调导致的问题
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
    }

    // State-only
    private fun captchaLogin() = uiStateFlow.value.run {
        if (phone.isNullOrBlank() || captcha.isNullOrBlank()) {
            logout("请检查输入")
        } else
            loginModel.captchaLogin(phone, captcha,
                {
                    it.run {
                        if (code == 200) {
                            login(it, "欢迎回来! ${profile.nickname}")
                        } else {
                            logout("验证码错误")
                        }
                    }
                },
                { code, _ ->
                    logout(if (code != ON_FAILURE_CODE) "验证码错误" else "网络错误, 请重试")
                })
    }

    /**
     * 本地化存储登录信息 + 更新 state
     */
    private fun login(response: LoginResponse, msg: String) {
        toast(msg)

        localLogin(response)
        stateLogin(response)
    }

    // 本地化存储登录信息
    private fun localLogin(response: LoginResponse) = viewModelScope.launch {
        response.apply {
            putStringData(preferenceUid, profile.userId)
            putStringData(preferenceAvatar, profile.avatarUrl)
            putStringData(preferenceBackground, profile.backgroundUrl)
            putStringData(preferenceNickname, profile.nickname)
            putStringData(preferenceCookie, cookie)
        }
    }

    // 更新 state
    private fun stateLogin(response: LoginResponse) {
        response.run {
            updateState {
                copy(
                    loggedInDatas = LoggedInDatas(
                        profile.userId,
                        profile.nickname,
                        cookie,
                        profile.avatarUrl,
                        profile.backgroundUrl
                    ),
                    isLoggedIn = true
                )
            }
        }
    }

    /**
     * 移除登录信息 + 更新 state
     */
    private fun logout(msg: String = "") = viewModelScope.launch {
        toastSuspended(msg)

        localLogout()
        stateLogout()
    }

    // 移除本地登录信息
    private fun localLogout() = viewModelScope.launch {
        putStringData(preferenceUid, "")
        putStringData(preferenceAvatar, "")
        putStringData(preferenceBackground, "")
        putStringData(preferenceNickname, "")
        putStringData(preferenceCookie, "")
    }

    // 更新 state
    private fun stateLogout() =
        updateState { copy(loggedInDatas = null, isLoggedIn = false, likeList = null) }

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
            loginModel.loginRefresh(loggedInDatas.cookie, // 状态码不为 200 且没有网络问题时登出
                {
                    if (it.code != 200)
                        logout("身份验证失败! 请重新登录")
                },
                { code, _ ->
                    if (code != ON_FAILURE_CODE)
                        logout("身份验证失败! 请重新登录")
                })
    }

    // State-only
    private fun getLikePlaylist() = uiStateFlow.value.loggedInDatas.run {
        if (this != null)
            playlistModel.getLikePlaylist(userId, cookie,
                {
                    getSongsWithIds(it.ids) { list ->
                        updateState { copy(likeList = list) }
                    }
                },
                { _, _ ->
                    updateState { copy(likeList = null) }
                })
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


    private fun stateLogin(
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

}