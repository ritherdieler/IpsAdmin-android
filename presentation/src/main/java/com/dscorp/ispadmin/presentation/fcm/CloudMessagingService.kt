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
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.presentation.ui.features.main.MainActivity
import com.dscorp.ispadmin.presentation.ui.features.main.permissions.FcmTopicManager
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.TICKET_ID
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.TicketActivity
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val TAG = "FcmService"

/**
 * Servicio principal de mensajería FCM que maneja las notificaciones push
 */
class CloudMessagingService : FirebaseMessagingService() {
    private val repository: IRepository by inject()
    private val notificationManager: NotificationManagerWrapper by lazy {
        NotificationManagerWrapperImpl(this)
    }
    private val messageHandlerRegistry: MessageHandlerRegistry by lazy {
        MessageHandlerRegistryImpl().apply { registerDefaultHandlers(this@CloudMessagingService) }
    }

    // Proporcionar acceso al repository para los handlers
    val repositoryAccess: IRepository get() = repository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Guardar el nuevo token en el servidor
        val user = repository.getUserSession()
        if (user != null && user.id != null) {
            try {
                // Uso de un scope personalizado para las corrutinas en el servicio
                MainScope().launch {
                    repository.updateDeviceToken(user.id!!, token)
                    Log.d(TAG, "Token FCM actualizado automáticamente en el servidor")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar token FCM: ${e.message}", e)
            }
        } else {
            Log.d(TAG, "No se pudo actualizar el token FCM: usuario no autenticado")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val fcmMessage = getFcmMessage(remoteMessage)

        if (fcmMessage != null) {
            // Crear un canal de notificación para Android 8.0+
            notificationManager.createNotificationChannel()

            // Procesar el mensaje usando el handler apropiado
            val handler = messageHandlerRegistry.getHandler(fcmMessage.type)

            handler?.handleMessage(fcmMessage, remoteMessage)
        }

        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }
    }

    private fun getFcmMessage(remoteMessage: RemoteMessage): FcmMessage? {
        val bodyData = remoteMessage.data["body"]
        return Gson().fromJson(bodyData, FcmMessage::class.java)
    }

    companion object {
        const val CHANNEL_ID = "MyChannelId"
        const val NOTIFICATION_ID = 1
    }
}

/**
 * Clase de datos que representa un mensaje FCM
 */
data class FcmMessage(
    val title: String,
    val type: FcmMessageType,
    val message: String,
    val id: String,
    val data: Any,
    /** Summary the Core sends for new tickets to read aloud; null from older Cores. */
    val speech: String? = null
)

/**
 * Tipos de mensajes FCM soportados
 */
enum class FcmMessageType {
    PAYMENT, ADVERTISING, GENERAL, INFO, ASSISTANCE_TICKET, PAYMENT_CRITICAL,
    PAYMENT_WARNING, PAYMENT_INFO, PAYMENT_SUCCESS, APP_MANAGEMENT, INSTALLATION_ORDER, TECHNICIAN_ASSIGNED_INSTALLATION_ORDER, SALES_ASSIGNED_INSTALLATION_ORDER, SALES_CLOSED_INSTALLATION_ORDER
}

// Keys para la gestión de acciones
const val MANAGEMENT_ACTION = "action"

// Acciones disponibles
const val FORCE_LOGOUT = "force_logout"

/**
 * Interfaz para la gestión de notificaciones
 */
interface NotificationManagerWrapper {
    fun createNotificationChannel()
    fun showNotification(
        title: String,
        message: String,
        pendingIntent: PendingIntent? = null,
        notificationId: Int = CloudMessagingService.NOTIFICATION_ID,
    )
}

/**
 * Implementación de gestión de notificaciones
 */
class NotificationManagerWrapperImpl(private val context: Context) : NotificationManagerWrapper {

    override fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "GigafiberPeru"
            val descriptionText = "Por favor, activa las notificaciones para recibir alertas"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(CloudMessagingService.CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun showNotification(
        title: String,
        message: String,
        pendingIntent: PendingIntent?,
        notificationId: Int,
    ) {
        // Crear una notificación
        val notificationBuilder =
            NotificationCompat.Builder(context, CloudMessagingService.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setColor(
                    ContextCompat.getColor(
                        context,
                        R.color.colorPrimary
                    )
                ) // Establecer el color aquí
                .setAutoCancel(true)


        // Mostrar la notificación
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(notificationId, notificationBuilder.build())
        }
    }
}

/**
 * Interfaz para manejar diferentes tipos de mensajes FCM
 */
interface MessageHandler {
    fun canHandle(messageType: FcmMessageType): Boolean
    fun handleMessage(message: FcmMessage, rawMessage: RemoteMessage)
}

/**
 * Registro de handlers para diferentes tipos de mensajes
 */
interface MessageHandlerRegistry {
    fun registerHandler(handler: MessageHandler)
    fun getHandler(messageType: FcmMessageType): MessageHandler?
}

/**
 * Implementación del registro de handlers
 */
class MessageHandlerRegistryImpl : MessageHandlerRegistry {
    private val handlers = mutableListOf<MessageHandler>()

    override fun registerHandler(handler: MessageHandler) {
        handlers.add(handler)
    }

    override fun getHandler(messageType: FcmMessageType): MessageHandler? {
        return handlers.find { it.canHandle(messageType) }
    }

    fun registerDefaultHandlers(service: CloudMessagingService) {
        registerHandler(AssistanceTicketHandler(service))
        registerHandler(InstallationOrderHandler(service))
        registerHandler(AppManagementHandler(service))
        registerHandler(DefaultNotificationHandler(service))
    }
}

/**
 * Handler para mensajes de tipo ASSISTANCE_TICKET
 */
class AssistanceTicketHandler(private val service: CloudMessagingService) : MessageHandler {
    private val notificationManager: NotificationManagerWrapper by lazy {
        NotificationManagerWrapperImpl(
            service
        )
    }
    override fun canHandle(messageType: FcmMessageType): Boolean {
        return messageType == FcmMessageType.ASSISTANCE_TICKET
    }

    override fun handleMessage(message: FcmMessage, rawMessage: RemoteMessage) {
        val intent = Intent(service, TicketActivity::class.java).apply {
            putExtra(TICKET_ID, message.id)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        // One notification and one tap target per ticket: a shared id replaced the previous
        // ticket's notification and reused its PendingIntent, which opened the older ticket.
        val ticketId = message.id.toIntOrNull() ?: CloudMessagingService.NOTIFICATION_ID
        val pendingIntent = PendingIntent.getActivity(
            service, ticketId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        notificationManager.showNotification(message.title, message.message, pendingIntent, ticketId)

        // Only new tickets come on this topic (status changes use the admins topic).
        val topic = rawMessage.from?.substringAfterLast("/", "")
        if (topic == FcmTopics.ASSISTANCE_TICKET) {
            VoiceAnnouncer.get(service).announce(ticketSpeech(message))
        }
    }
}

/**
 * Handler para mensajes de tipo INSTALLATION_ORDER
 */
class InstallationOrderHandler(private val service: CloudMessagingService) : MessageHandler {
    private val notificationManager: NotificationManagerWrapper by lazy {
        NotificationManagerWrapperImpl(
            service
        )
    }
    private val voice: VoiceAnnouncer by lazy { VoiceAnnouncer.get(service) }

    override fun canHandle(messageType: FcmMessageType): Boolean {
        return messageType == FcmMessageType.INSTALLATION_ORDER ||
                messageType == FcmMessageType.TECHNICIAN_ASSIGNED_INSTALLATION_ORDER ||
                messageType == FcmMessageType.SALES_ASSIGNED_INSTALLATION_ORDER ||
                messageType == FcmMessageType.SALES_CLOSED_INSTALLATION_ORDER
    }

    override fun handleMessage(message: FcmMessage, rawMessage: RemoteMessage) {
        notificationManager.showNotification(message.title, message.message)

        // Verificar el tipo de mensaje y reproducir mensaje de voz adecuado
        when (message.type) {
            FcmMessageType.INSTALLATION_ORDER -> {
                // Verificar el topic y reproducir mensaje de voz si corresponde
                val topic = rawMessage.from?.substringAfterLast("/", "")
                if (topic == FcmTopics.TOPIC_INSTALLATION_ORDER) {
                    voice.announce(message.message)
                }
            }

            FcmMessageType.TECHNICIAN_ASSIGNED_INSTALLATION_ORDER,
            FcmMessageType.SALES_CLOSED_INSTALLATION_ORDER,
            FcmMessageType.SALES_ASSIGNED_INSTALLATION_ORDER -> {
                // Leer el mensaje recibido con voz para estos tipos específicos
                voice.announce(message.message)
            }

            else -> { /* No hacer nada para otros tipos */
            }
        }
    }
}

/**
 * Handler para mensajes de tipo APP_MANAGEMENT
 */
class AppManagementHandler(private val service: CloudMessagingService) : MessageHandler {
    private val repository: IRepository by lazy { service.repositoryAccess }

    override fun canHandle(messageType: FcmMessageType): Boolean {
        return messageType == FcmMessageType.APP_MANAGEMENT
    }

    override fun handleMessage(message: FcmMessage, rawMessage: RemoteMessage) {
        val map = message.data as? Map<String, String> ?: return
        val action = map[MANAGEMENT_ACTION]

        when (action) {
            FORCE_LOGOUT -> {
                repository.clearUserSession()
                FcmTopicManager(FirebaseMessaging.getInstance()).unsubscribeFromRoleTopics()
                val intent = Intent(service, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                service.startActivity(intent)
            }

            else -> {
                Log.d(TAG, "No se encontró ninguna acción")
            }
        }
    }
}

/**
 * Handler predeterminado para tipos de mensajes simples que solo muestran notificaciones
 */
class DefaultNotificationHandler(private val service: CloudMessagingService) : MessageHandler {
    private val notificationManager: NotificationManagerWrapper by lazy {
        NotificationManagerWrapperImpl(
            service
        )
    }

    override fun canHandle(messageType: FcmMessageType): Boolean {
        return messageType in listOf(
            FcmMessageType.PAYMENT,
            FcmMessageType.ADVERTISING,
            FcmMessageType.GENERAL,
            FcmMessageType.INFO,
            FcmMessageType.PAYMENT_CRITICAL,
            FcmMessageType.PAYMENT_WARNING,
            FcmMessageType.PAYMENT_INFO,
            FcmMessageType.PAYMENT_SUCCESS
        )
    }

    override fun handleMessage(message: FcmMessage, rawMessage: RemoteMessage) {
        notificationManager.showNotification(message.title, message.message)
    }
}
