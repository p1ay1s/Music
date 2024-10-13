package com.niki.music.my.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.niki.common.ui.BaseBottomSheetDialogFragment
import com.niki.music.R
import com.niki.music.appLoadingDialog
import com.niki.music.common.views.IView
import com.niki.music.databinding.FragmentLoginBinding
import com.niki.music.my.MyEffect
import com.niki.music.my.MyIntent
import com.niki.music.my.MyViewModel
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

class LoginFragment : BaseBottomSheetDialogFragment(R.layout.fragment_login), IView,
    DismissCallback {
    private lateinit var myViewModel: MyViewModel

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

        myViewModel = ViewModelProvider(requireActivity())[MyViewModel::class.java]

        dismissCallback = this
        handle()

        binding.run {
            editPhone.editText?.addTextChangedListener {
                updatePhone(it.toString())
                myViewModel.sendIntent(MyIntent.GetAvatarUrl)
            }
            editPassword.editText?.addTextChangedListener { updateCaptcha(it.toString()) }
            getCodeButton.setOnClickListener { myViewModel.sendIntent(MyIntent.SendCaptcha) }
            switchMethod.setOnClickListener {
                myViewModel.sendIntent(MyIntent.SwitchMethod)
            }
            loginButton.setOnClickListener {
                if (myViewModel.uiStateFlow.value.useCaptcha)
                    captchaLogin()
                else
                    passwordLogin()
            }
        }
    }

    private fun captchaLogin() {
        appLoadingDialog?.show()
        myViewModel.sendIntent(MyIntent.CaptchaLogin)
    }

    private fun passwordLogin() {
        appLoadingDialog?.show()
        myViewModel.sendIntent(MyIntent.PasswordLogin)
    }

    override fun handle() =
        lifecycleScope.apply {
            effectJob?.cancel()
            effectJob = launch {
                myViewModel.uiEffectFlow
                    .collect {
                        logE(
                            TAG,
                            "COLLECTED ${it::class.qualifiedName.toString()}"
                        )
                        when (it) {
                            is MyEffect.GetAvatarUrlOkEffect -> {
                                binding.userAvatar.setCircleImgView(
                                    it.url,
                                    false
                                )
                            }

                            MyEffect.GetAvatarUrlBadEffect -> {
                                binding.userAvatar.visibility =
                                    View.INVISIBLE
                            }
                        }
                    }
            }

            myViewModel.observeState {
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

    private fun updatePhone(phone: String) = myViewModel.sendIntent(MyIntent.UpdatePhone(phone))
    private fun updateCaptcha(captcha: String) =
        myViewModel.sendIntent(MyIntent.UpdateCaptcha(captcha))

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