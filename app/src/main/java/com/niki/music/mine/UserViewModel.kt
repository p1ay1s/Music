package com.niki.music.mine

import androidx.lifecycle.viewModelScope
import com.niki.common.repository.dataclasses.login.loginApi.LoginResponse
import com.niki.common.utils.getStringData
import com.niki.common.utils.putStringData
import com.niki.common.values.preferenceAvatar
import com.niki.common.values.preferenceBackground
import com.niki.common.values.preferenceCookie
import com.niki.common.values.preferenceNickname
import com.niki.common.values.preferenceUid
import com.niki.music.appLoadingDialog
import com.niki.music.mine.login.LoginModel
import com.niki.music.viewModel.BaseViewModel
import com.p1ay1s.base.extension.TAG
import com.p1ay1s.base.extension.toast
import com.p1ay1s.base.extension.toastSuspended
import com.p1ay1s.base.log.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

var appCookie = ""

class UserViewModel : BaseViewModel<UserIntent, UserState, UserEffect>() {
    private val loginModel by lazy { LoginModel() }

    private var avatarJob: Job? = null

    companion object {
        const val MAX_ALBUM_COUNT = 20
    }

    override fun initUiState() =
        UserState(null, null, isLoggedIn = false, useCaptcha = true, null, null, null)

    init {
        initLoginState()
    }

    override fun handleIntent(intent: UserIntent) {
        intent.run {
            logE(TAG, "RECEIVED " + this::class.simpleName.toString())
            when (this) {
                is UserIntent.UpdatePhone -> updateState { copy(phone = this@run.phone) }
                is UserIntent.UpdateCaptcha -> updateState { copy(captcha = this@run.captcha) }
                UserIntent.GetAvatarUrl -> getAvatarUrl()
                UserIntent.CaptchaLogin -> captchaLogin()
                UserIntent.GetLikePlaylist -> getLikePlaylist()
                UserIntent.GetLikeAlbums -> getLikeAlbums()
                UserIntent.SendCaptcha -> sendCaptcha()
                UserIntent.Logout -> logout()
                UserIntent.PasswordLogin -> passwordLogin()
                UserIntent.SwitchMethod -> updateState {
                    copy(useCaptcha = !useCaptcha)
                }
            }
        }
    }

    // Effect-only
    private fun getAvatarUrl() = state.run {
        if (phone.isNullOrBlank()) return@run

        viewModelScope.launch {
            avatarJob?.cancel()
            avatarJob?.join()
            avatarJob = launch(Dispatchers.IO) {
                delay(300) // 冷静期, 避免在某些情况下前一个请求反而后回调导致的问题
                loginModel.getAvatarUrl(phone,
                    {
                        if (it.exist == 1)
                            sendEffect { UserEffect.GetAvatarUrlOkEffect(it.avatarUrl) }
                        else
                            sendEffect { UserEffect.GetAvatarUrlBadEffect }
                    },
                    { _, _ ->
                        sendEffect { UserEffect.GetAvatarUrlBadEffect }
                    })
            }
        }
    }

    // State-only
    private fun captchaLogin() = state.run {
        if (phone.isNullOrBlank() || captcha.isNullOrBlank()) {
            logout("请检查输入")
        } else
            loginModel.captchaLogin(phone, captcha,
                {
                    appLoadingDialog?.dismiss()
                    it.run {
                        if (code == 200) {
                            login(it, "欢迎回来! ${profile.nickname}")
                        } else {
                            if (message != null) logout(message!!) else logout()
                        }
                    }
                },
                { code, _ ->
                    appLoadingDialog?.dismiss()
                    code?.let {
                        logout(code.toString())
                    } ?: logout("网络错误")
                })
    }

    private fun passwordLogin() = state.run {
        if (phone.isNullOrBlank() || captcha.isNullOrBlank()) {
            logout("请检查输入")
        } else
            loginModel.passwordLogin(phone, captcha,
                {
                    appLoadingDialog?.dismiss()
                    it.run {
                        if (code == 200) {
                            login(it, "欢迎回来! ${profile.nickname}")
                        } else {
                            if (message != null) logout(message!!) else logout()
                        }
                    }
                },
                { code, _ ->
                    appLoadingDialog?.dismiss()
                    code?.let {
                        logout(code.toString())
                    } ?: logout("网络错误")
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

    private fun sendCaptcha() = state.run {
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
    private fun checkCookieAbility() {
        if (appCookie.isNotBlank())
            loginModel.loginRefresh(
                appCookie, // 状态码不为 2xx 且没有网络问题时登出
                {},
                { code, _ ->
                    code?.let {
                        logE("###", it.toString())
                        logout("身份验证失败! 请重新登录")
                    }
                })
    }

    // State-only
    private fun getLikePlaylist() = state.loggedInDatas.run {
        if (this != null)
            playlistModel.getLikePlaylist(userId, cookie,
                {
                    runCatching {
                        getSongsWithIds(it.ids) { list ->
                            updateState { copy(likeList = list) }
                        }
                    }
                },
                { _, _ ->
                    updateState { copy(likeList = null) }
                })
    }

    private fun getLikeAlbums() = state.loggedInDatas.run {
        if (this != null)
            playlistModel.getLikeAlbums(cookie, MAX_ALBUM_COUNT, 0,
                {
                    runCatching {
                        updateState { copy(likeAlbums = it.data) }
                    }.onFailure {
                        updateState { copy(likeAlbums = null) }
                    }
                },
                { _, _ ->
                    updateState { copy(likeAlbums = null) }
                })
    }

    private fun initLoginState() =
        viewModelScope.launch {
            val userId = getStringData(preferenceUid)
            val nickname = getStringData(preferenceNickname)
            val cookie = getStringData(preferenceCookie)
            val avatarUrl = getStringData(preferenceAvatar)
            val backgroundUrl = getStringData(preferenceBackground)

            appCookie = cookie

            if (cookie.isNotBlank()) {
                updateState {
                    copy(
                        loggedInDatas = LoggedInDatas(
                            userId,
                            nickname,
                            cookie,
                            avatarUrl,
                            backgroundUrl
                        ), isLoggedIn = true
                    )
                }
                checkCookieAbility()
            }
        }
}