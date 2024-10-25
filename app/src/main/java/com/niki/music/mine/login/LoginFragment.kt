package com.niki.music.mine.login

import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.niki.music.appLoadingDialog
import com.niki.music.databinding.FragmentLoginBinding
import com.niki.music.mine.UserEffect
import com.niki.music.mine.UserIntent
import com.niki.music.mine.UserViewModel
import com.p1ay1s.base.extension.loadCircleImage
import com.p1ay1s.impl.ui.ViewBindingDialogFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LoginFragment : ViewBindingDialogFragment<FragmentLoginBinding>(1.0) {
    private lateinit var userViewModel: UserViewModel

    private var effectJob: Job? = null

    override fun FragmentLoginBinding.initBinding() {
        userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
        handle()

        editPhone.editText?.addTextChangedListener {
            updatePhone(it.toString())
            userViewModel.sendIntent(UserIntent.GetAvatarUrl)
        }
        editPassword.editText?.addTextChangedListener { updateCaptcha(it.toString()) }
        getCodeButton.setOnClickListener { userViewModel.sendIntent(UserIntent.SendCaptcha) }
        switchMethodButton.setOnClickListener {
            userViewModel.sendIntent(UserIntent.SwitchMethod)
        }
        loginButton.setOnClickListener {
            if (userViewModel.state.useCaptcha)
                captchaLogin()
            else
                passwordLogin()
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
                        when (it) {
                            is UserEffect.GetAvatarUrlOkEffect ->
                                binding.userAvatar.loadCircleImage(it.url)

                            UserEffect.GetAvatarUrlBadEffect ->
                                binding.userAvatar.visibility =
                                    View.INVISIBLE
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
                                switchMethodButton.text = "密码登录"
                                editPassword.hint = "验证码"
                            } else {
                                getCodeButton.visibility = View.INVISIBLE
                                switchMethodButton.text = "验证码登录"
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
        effectJob?.cancel()
        effectJob = null
    }
}