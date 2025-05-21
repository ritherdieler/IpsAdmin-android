package com.dscorp.ispadmin.presentation.ui.features.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.databinding.ActivityLoginBinding
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.extension.showCrossDialog
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.features.login.compose.LoginScreen
import com.dscorp.ispadmin.presentation.ui.features.main.ComposeMainActivity
import com.dscorp.ispadmin.presentation.ui.features.main.MainActivity
import com.dscorp.ispadmin.presentation.ui.features.registration.RegisterActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.androidx.viewmodel.ext.android.viewModel


class LoginActivity : AppCompatActivity() {

    val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }

    val viewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        binding.composeView.setContent {
            MyTheme {
                LoginScreen(
                    onCreatedAccountClicked = ::navigateToRegister,
                    onLoginSuccess = ::handleLoginResponse
                )
            }
        }

        setContentView(binding.root)
    }

    private fun handleLoginResponse(user: User) {
        if (!user.verified)
            showCrossDialog(R.string.your_account_isnt_verified, lottieRes = R.raw.info)
        else {
            FirebaseCrashlytics.getInstance().setUserId(user.id.toString())
            val intent = Intent(this, ComposeMainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}
