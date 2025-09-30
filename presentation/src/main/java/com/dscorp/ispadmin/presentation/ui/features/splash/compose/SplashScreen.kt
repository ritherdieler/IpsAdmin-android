package com.dscorp.ispadmin.presentation.ui.features.splash.compose

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
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.ui.components.Loader
import com.dscorp.ispadmin.presentation.ui.features.dialog.MyConfirmDialog
import com.dscorp.ispadmin.presentation.ui.features.login.CheckVersionState
import com.dscorp.ispadmin.presentation.ui.features.login.LoginViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SplashScreen(
    onNavigateToMain: (User) -> Unit,
    onNavigateToLogin: () -> Unit,
    onFinishApp: () -> Unit,
    viewModel: LoginViewModel = koinViewModel()
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