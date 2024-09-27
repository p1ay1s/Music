package com.niki.music.my.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.niki.base.view.ui.BaseBottomSheetDialogFragment
import com.niki.music.R
import com.niki.music.databinding.FragmentLoginBinding
import com.niki.music.my.MyEffect
import com.niki.music.my.MyIntent
import com.niki.music.my.MyViewModel
import com.p1ay1s.dev.base.TAG
import com.p1ay1s.dev.log.logE
import com.p1ay1s.dev.util.ImageSetter.setCircleImgView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LoginFragment : BaseBottomSheetDialogFragment(R.layout.fragment_login) {
    private val myViewModel: MyViewModel by activityViewModels<MyViewModel>()

    private lateinit var mBinding: FragmentLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentLoginBinding.inflate(inflater)
        return mBinding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleStates()

        mBinding.run {
            editPhone.editText?.addTextChangedListener {
                updatePhone(it.toString())
                myViewModel.sendIntent(MyIntent.GetAvatarUrl)
            }
            editPassword.editText?.addTextChangedListener { updateCaptcha(it.toString()) }
            getCodeButton.setOnClickListener { myViewModel.sendIntent(MyIntent.SendCaptcha) }
            loginButton.setOnClickListener { myViewModel.sendIntent(MyIntent.CaptchaLogin) }
        }
    }

    private fun handleStates() =
        lifecycleScope.apply {
            launch {
                myViewModel.uiEffectFlow
                    .collect {
                        logE(
                            TAG,
                            "collected ${it::class.qualifiedName.toString()}"
                        )
                        when (it) {
                            is MyEffect.GetAvatarUrlOkEffect -> {
                                mBinding.userAvatar.setCircleImgView(
                                    it.url,
                                    false
                                )
                            }

                            MyEffect.GetAvatarUrlBadEffect -> {
                                mBinding.userAvatar.visibility =
                                    View.INVISIBLE
                            }

                            else -> {}
                        }
                    }
            }
            myViewModel.observeState {
                launch {
                    map { it.isLoggedIn }.distinctUntilChanged().collect {
                        if (it)
                            dismiss()
                    }
                }
            }
        }

    private fun updatePhone(phone: String) = myViewModel.sendIntent(MyIntent.UpdatePhone(phone))
    private fun updateCaptcha(captcha: String) =
        myViewModel.sendIntent(MyIntent.UpdateCaptcha(captcha))
}