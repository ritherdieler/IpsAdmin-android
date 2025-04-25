package com.dscorp.ispadmin.presentation.ui.features.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.features.dialog.MyConfirmDialog
import com.dscorp.ispadmin.presentation.ui.features.login.CheckVersionState
import com.dscorp.ispadmin.presentation.ui.features.login.LoginActivity
import com.dscorp.ispadmin.presentation.ui.features.login.LoginViewModel
import com.dscorp.ispadmin.presentation.ui.features.main.MainActivity
import com.dscorp.ispadmin.presentation.ui.features.migration.Loader
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.androidx.viewmodel.ext.android.viewModel

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        setContent {
            MyTheme {
                SplashScreen(
                    onNavigateToMain = { user -> navigateToMainActivity(user) },
                    onNavigateToLogin = { navigateToLoginActivity() },
                    onFinishApp = { finish() },
                    viewModel = viewModel
                )
            }
        }
    }

    private fun navigateToMainActivity(user: User) {
        FirebaseCrashlytics.getInstance().setUserId(user.id.toString())
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

@Composable
fun SplashScreen(
    onNavigateToMain: (User) -> Unit,
    onNavigateToLogin: () -> Unit,
    onFinishApp: () -> Unit,
    viewModel: LoginViewModel
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Loader()
            
            // Observe version check state
            val checkVersionState by viewModel.checkVersionFlow.collectAsState()
            
            // Track if update dialog is showing
            var showUpdateDialog by remember { mutableStateOf(false) }
            
            LaunchedEffect(key1 = Unit) {
                viewModel.checkAppVersion()
            }
            
            // React to version check state
            LaunchedEffect(key1 = checkVersionState) {
                when (val state = checkVersionState) {
                    is CheckVersionState.CheckVersionSuccess -> {
                        if (state.forceUpdate) {
                            // Need to update app - show dialog
                            showUpdateDialog = true
                        } else {
                            // No update needed, proceed with session check
                            val (sessionExists, user) = viewModel.checkSessionStatus()
                            if (sessionExists && user != null && user.verified) {
                                onNavigateToMain(user)
                            } else {
                                onNavigateToLogin()
                            }
                        }
                    }
                    is CheckVersionState.Error -> {
                        // Error checking version - proceed with normal flow
                        val (sessionExists, user) = viewModel.checkSessionStatus()
                        if (sessionExists && user != null && user.verified) {
                            onNavigateToMain(user)
                        } else {
                            onNavigateToLogin()
                        }
                    }
                    CheckVersionState.Loading -> {
                        // Wait for version check to complete
                    }
                }
            }
            
            // Show update dialog if needed
            if (showUpdateDialog) {
                MyConfirmDialog(
                    title = "Hay una nueva versión disponible",
                    body = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Es necesario actualizar la aplicación para continuar",
                            textAlign = TextAlign.Center
                        )
                    },
                    onAccept = {
                        onFinishApp()
                    },
                    onDismissRequest = { } // No permitir cerrar sin actualizar
                )
            }
        }
    }
} 