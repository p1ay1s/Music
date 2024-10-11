package com.niki.music.my.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
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
    private var mHandleJob: Job? = null

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
            loginButton.setOnClickListener {
                appLoadingDialog?.show()
                myViewModel.sendIntent(MyIntent.CaptchaLogin)
            }
        }
    }

    override fun handle() =
        lifecycleScope.apply {
            mHandleJob?.cancel()
            mHandleJob = launch {
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

                            else -> {}
                        }
                    }
            }

            myViewModel.observeState {
                launch {
                    map { it.isLoggedIn }.distinctUntilChanged().collect {
                        if (it) {
                            appLoadingDialog?.dismiss()
                            dismiss()
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
        mHandleJob?.cancel()
        mHandleJob = null
    }

    override fun dismissDialog() {
        dismiss()
    }
}