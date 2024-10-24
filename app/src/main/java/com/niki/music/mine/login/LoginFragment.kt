package com.niki.music.mine.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.niki.music.R
import com.niki.music.appLoadingDialog
import com.niki.music.ui.BaseBottomSheetDialogFragment
import com.niki.music.databinding.FragmentLoginBinding
import com.niki.music.mine.UserEffect
import com.niki.music.mine.UserIntent
import com.niki.music.mine.UserViewModel
import com.p1ay1s.base.extension.TAG
import com.p1ay1s.base.log.logE
import com.p1ay1s.util.ImageSetter.setCircleImgView
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

fun interface DismissCallback {
    fun dismissDialog()
}

// 会被及时释放
var dismissCallback: DismissCallback? = null

class LoginFragment : BaseBottomSheetDialogFragment(R.layout.fragment_login), DismissCallback {
    private lateinit var userViewModel: UserViewModel

    private lateinit var binding: FragmentLoginBinding
    private var effectJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]

        dismissCallback = this
        handle()

        binding.run {
            editPhone.editText?.addTextChangedListener {
                updatePhone(it.toString())
                userViewModel.sendIntent(UserIntent.GetAvatarUrl)
            }
            editPassword.editText?.addTextChangedListener { updateCaptcha(it.toString()) }
            getCodeButton.setOnClickListener { userViewModel.sendIntent(UserIntent.SendCaptcha) }
            switchMethod.setOnClickListener {
                userViewModel.sendIntent(UserIntent.SwitchMethod)
            }
            loginButton.setOnClickListener {
                if (userViewModel.state.useCaptcha)
                    captchaLogin()
                else
                    passwordLogin()
            }
        }
    }

    private fun captchaLogin() {
        appLoadingDialog?.show()
        userViewModel.sendIntent(UserIntent.CaptchaLogin)
    }

    private fun passwordLogin() {
        appLoadingDialog?.show()
        userViewModel.sendIntent(UserIntent.PasswordLogin)
    }

    private fun handle() =
        lifecycleScope.apply {
            effectJob?.cancel()
            effectJob = launch {
                userViewModel.uiEffectFlow
                    .collect {
                        logE(
                            TAG,
                            "COLLECTED ${it::class.qualifiedName.toString()}"
                        )
                        when (it) {
                            is UserEffect.GetAvatarUrlOkEffect -> {
                                binding.userAvatar.setCircleImgView(
                                    it.url,
                                    false
                                )
                            }

                            UserEffect.GetAvatarUrlBadEffect -> {
                                binding.userAvatar.visibility =
                                    View.INVISIBLE
                            }
                        }
                    }
            }

            userViewModel.observeState {
                launch {
                    map { it.isLoggedIn }.distinctUntilChanged().collect {
                        if (it) {
                            dismiss()
                        }
                    }
                }
                launch {
                    map { it.useCaptcha }.distinctUntilChanged().collect {
                        binding.apply {
                            if (it) {
                                getCodeButton.visibility = View.VISIBLE
                                switchMethod.text = "密码登录"
                                editPassword.hint = "验证码"
                            } else {
                                getCodeButton.visibility = View.INVISIBLE
                                switchMethod.text = "验证码登录"
                                editPassword.hint = "密码"
                            }
                        }
                    }
                }
            }
        }

    private fun updatePhone(phone: String) = userViewModel.sendIntent(UserIntent.UpdatePhone(phone))
    private fun updateCaptcha(captcha: String) =
        userViewModel.sendIntent(UserIntent.UpdateCaptcha(captcha))

    override fun onDestroyView() {
        super.onDestroyView()
        dismissCallback = null
        effectJob?.cancel()
        effectJob = null
    }

    override fun dismissDialog() {
        dismiss()
    }
}