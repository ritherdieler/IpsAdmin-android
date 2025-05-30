package com.dscorp.ispadmin.presentation.ui.features.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.UpdateDeviceTokenUseCase
import com.dscorp.ispadmin.navigation.IpsAdminNavHost
import com.dscorp.ispadmin.presentation.fcm.FcmTopics
import com.dscorp.ispadmin.presentation.fcm.updateFcmToken
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.features.main.permissions.FcmTopicManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private lateinit var fcmTopicManager: FcmTopicManager
    private val viewModel: MainActivityViewModel by inject()
    private val firebaseAnalytics: FirebaseAnalytics by inject()
    private val updateDeviceTokenUseCase: UpdateDeviceTokenUseCase by inject()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNotificationPermission()
        fcmTopicManager = FcmTopicManager(FirebaseMessaging.getInstance())
        setupUserProfile()

        updateFcmToken()

        setContent {
            MyTheme {
                IpsAdminNavHost()
            }
        }


    }

    private fun updateFcmToken() {
        val currentUser = viewModel.getCurrentUser()
        currentUser?.let { user ->
            lifecycleScope.launch {
                updateFcmToken(
                    context = this@MainActivity,
                    user = user,
                    updateDeviceTokenUseCase = updateDeviceTokenUseCase
                )
            }
        }
    }

    private fun setupUserProfile() {
        // Suscribirse al tema común para todos los usuarios
        FirebaseMessaging.getInstance().subscribeToTopic(FcmTopics.FCM_ALL_TOPIC)

        viewModel.user?.let { user ->
            firebaseAnalytics.setUserId(user.id.toString())

            // Aplicar configuración de permisos y suscripción a tópicos FCM
            applyUserConfiguration(user)

            // Aquí podríamos volver a habilitar la navegación inicial si se desea
            // navigateToInitialDestination(user)
        }
    }

    private fun applyUserConfiguration(user: User) {
        // Configurar suscripciones FCM
        fcmTopicManager.subscribeToTopicsForUserType(user.type!!)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                PERMISSION_CODE
            )
        }
    }


}

const val PERMISSION_CODE = 1001