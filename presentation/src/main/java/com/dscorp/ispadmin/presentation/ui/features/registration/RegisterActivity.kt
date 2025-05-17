package com.dscorp.ispadmin.presentation.ui.features.registration

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.databinding.ActivityRegisterBinding
import com.dscorp.ispadmin.presentation.extension.showCrossDialog
import com.dscorp.ispadmin.presentation.ui.features.base.BaseActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class RegisterActivity : BaseActivity<RegisterUiState,ActivityRegisterBinding>() {

    override val binding by lazy { ActivityRegisterBinding.inflate(layoutInflater) }
    override val viewModel: RegisterViewModel by viewModel()

    override fun handleState(state: RegisterUiState) {
        when (state) {
            is RegisterUiState.OnRegister -> showCrossDialog(getString(R.string.user_register_success)) { finish() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.executePendingBindings()
    }


}
