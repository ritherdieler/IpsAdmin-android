package com.dscorp.ispadmin.presentation.fcm

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.messaging.RemoteMessage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTextToSpeech
import java.util.Locale

/** The push exactly as the Core's StaffTicketPush sends it, through the real FCM service. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TicketPushVoiceTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `new tickets are read aloud in order and each one gets its own notification`() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowTextToSpeech.addLanguageAvailability(Locale("es", "PE"))
        val service = Robolectric.buildService(CloudMessagingService::class.java).create().get()

        service.onMessageReceived(ticketPush(42, "Nuevo ticket, prioridad alta. Sin Conexión a Internet. Cliente Juan Pérez, por WhatsApp."))
        // The engine is still starting: the first ticket waits instead of being lost.
        val tts = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance())
        assertEquals(emptyList<String>(), tts.spokenTextList)
        tts.onInitListener.onInit(TextToSpeech.SUCCESS)
        service.onMessageReceived(ticketPush(43, "Nuevo ticket, prioridad media. Otros. Cliente Rosa Quispe, desde la app Mi GigaFiber."))
        // A status change goes to the admins topic and is not read aloud.
        service.onMessageReceived(ticketPush(42, "Ignorado", topic = FcmTopics.ASSISTANCE_TICKET_ADMINS))

        assertEquals(
            listOf(
                "Nuevo ticket, prioridad alta. Sin Conexión a Internet. Cliente Juan Pérez, por WhatsApp.",
                "Nuevo ticket, prioridad media. Otros. Cliente Rosa Quispe, desde la app Mi GigaFiber.",
            ),
            tts.spokenTextList,
        )
        assertEquals(TextToSpeech.QUEUE_ADD, tts.queueMode)
        val notifications = shadowOf(app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        assertEquals(setOf(42, 43), notifications.activeNotifications.map { it.id }.toSet())
    }

    private fun ticketPush(id: Int, speech: String, topic: String = FcmTopics.ASSISTANCE_TICKET): RemoteMessage {
        val body = """{"id":"$id","title":"Nuevo Ticket | Prioridad Alta","type":"ASSISTANCE_TICKET",""" +
            """"message":"Sin Conexión a Internet - Sin señal","data":"{}","topic":"$topic","speech":"$speech"}"""
        return RemoteMessage(Bundle().apply {
            putString("from", "/topics/$topic")
            putString("title", "Nuevo Ticket | Prioridad Alta")
            putString("message", "Sin Conexión a Internet - Sin señal")
            putString("body", body)
        })
    }
}
