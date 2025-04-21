package com.dscorp.ispadmin.presentation.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.ui.features.login.LoginActivity
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.TICKET_ID
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.TicketActivity
import com.example.data2.data.repository.IRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import org.koin.android.ext.android.inject

private const val TAG = "FcmService"

class CloudMessagingService : FirebaseMessagingService() {
    private val repository: IRepository by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val fcmMessage = getFcmMessage(remoteMessage)

        if (fcmMessage != null) {
            when (fcmMessage.type) {
                FcmMessageType.ASSISTANCE_TICKET -> {
                    val intent = Intent(this, TicketActivity::class.java).apply {
                        putExtra(TICKET_ID, fcmMessage.id)
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    val pendingIntent = PendingIntent.getActivity(
                        this, 0, intent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                    createNotification(fcmMessage.title, fcmMessage.message, pendingIntent)
                }

                FcmMessageType.PAYMENT,
                FcmMessageType.ADVERTISING,
                FcmMessageType.GENERAL,
                FcmMessageType.INFO,
                FcmMessageType.PAYMENT_CRITICAL,
                FcmMessageType.PAYMENT_WARNING,
                FcmMessageType.PAYMENT_INFO,
                FcmMessageType.INSTALLATION_ORDER,
                        FcmMessageType.PAYMENT_SUCCESS -> {
                    createNotification(fcmMessage.title, fcmMessage.message)
                }

                FcmMessageType.APP_MANAGEMENT -> {
                    val map = fcmMessage.data as Map<String, String>
                    val action = map[MANAGEMENT_ACTION]
                    when (action) {
                        FORCE_LOGOUT -> {
                            repository.clearUserSession()
                            val intent = Intent(this, LoginActivity::class.java).apply {
                                flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                        }

                        else -> {
                            Log.d(TAG, "No se encontró ninguna acción")
                        }
                    }
                }
            }

            // Crear un canal de notificación si estás en Android 8.0 (Oreo) o superior
            createNotificationChannel()
        }

        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }
    }

    private fun createNotification(
        title: String,
        message: String,
        pendingIntent: PendingIntent? = null
    ) {
        // Crear una notificación
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setColor(
                ContextCompat.getColor(
                    this,
                    R.color.colorPrimary
                )
            ) // Establecer el color aquí
            .setAutoCancel(true)


        // Mostrar la notificación
        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@CloudMessagingService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    private fun getFcmMessage(remoteMessage: RemoteMessage): FcmMessage? {
        val bodyData = remoteMessage.data["body"]
        return Gson().fromJson(bodyData, FcmMessage::class.java)
    }

    // Crear un canal de notificación (Android 8.0+)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "GigafiberPeru"
            val descriptionText = "Por favor, activa las notificaciones para recibir alertas"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "FcmService"
        private const val CHANNEL_ID = "MyChannelId"
        private const val NOTIFICATION_ID = 1
    }

}

data class FcmMessage(
    val title: String,
    val type: FcmMessageType,
    val message: String,
    val id: String,
    val data: Any
)

enum class FcmMessageType {
    PAYMENT, ADVERTISING, GENERAL, INFO, ASSISTANCE_TICKET, PAYMENT_CRITICAL, PAYMENT_WARNING, PAYMENT_INFO, PAYMENT_SUCCESS, APP_MANAGEMENT,INSTALLATION_ORDER
}

//keys
const val MANAGEMENT_ACTION = "action"

//ACTIONS
const val FORCE_LOGOUT = "force_logout"