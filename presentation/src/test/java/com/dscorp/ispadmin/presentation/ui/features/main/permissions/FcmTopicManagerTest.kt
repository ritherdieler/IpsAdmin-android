package com.dscorp.ispadmin.presentation.ui.features.main.permissions

import com.dscorp.ispadmin.domain.model.User.UserType
import com.dscorp.ispadmin.presentation.fcm.FcmTopics
import com.google.firebase.messaging.FirebaseMessaging
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class FcmTopicManagerTest {

    private val firebaseMessaging: FirebaseMessaging = mockk(relaxed = true)
    private val manager = FcmTopicManager(firebaseMessaging)

    @Test
    fun `subscribing as sales leaves topics of other roles`() {
        manager.subscribeToTopicsForUserType(UserType.SALES)

        verify { firebaseMessaging.subscribeToTopic(FcmTopics.TOPIC_INSTALLATION_ORDER) }
        verify { firebaseMessaging.unsubscribeFromTopic(FcmTopics.FCM_TECHNICIAN_TOPIC) }
        verify { firebaseMessaging.unsubscribeFromTopic(FcmTopics.FCM_SECRETARY_TOPIC) }
        verify { firebaseMessaging.unsubscribeFromTopic(FcmTopics.ASSISTANCE_TICKET) }
        verify { firebaseMessaging.unsubscribeFromTopic(FcmTopics.ASSISTANCE_TICKET_ADMINS) }
        verify(exactly = 0) { firebaseMessaging.unsubscribeFromTopic(FcmTopics.TOPIC_INSTALLATION_ORDER) }
    }

    @Test
    fun `subscribing as technician keeps its own topics`() {
        manager.subscribeToTopicsForUserType(UserType.TECHNICIAN)

        verify { firebaseMessaging.subscribeToTopic(FcmTopics.FCM_ALL_TOPIC) }
        verify { firebaseMessaging.subscribeToTopic(FcmTopics.FCM_TECHNICIAN_TOPIC) }
        verify { firebaseMessaging.subscribeToTopic(FcmTopics.ASSISTANCE_TICKET) }
        verify(exactly = 0) { firebaseMessaging.unsubscribeFromTopic(FcmTopics.FCM_TECHNICIAN_TOPIC) }
        verify(exactly = 0) { firebaseMessaging.unsubscribeFromTopic(FcmTopics.ASSISTANCE_TICKET) }
        verify { firebaseMessaging.unsubscribeFromTopic(FcmTopics.TOPIC_INSTALLATION_ORDER) }
    }

    @Test
    fun `logout unsubscribes from every role topic but keeps the global one`() {
        manager.unsubscribeFromRoleTopics()

        listOf(
            FcmTopics.FCM_TECHNICIAN_TOPIC,
            FcmTopics.FCM_SECRETARY_TOPIC,
            FcmTopics.ASSISTANCE_TICKET,
            FcmTopics.ASSISTANCE_TICKET_ADMINS,
            FcmTopics.TOPIC_INSTALLATION_ORDER,
        ).forEach { topic -> verify { firebaseMessaging.unsubscribeFromTopic(topic) } }
        verify(exactly = 0) { firebaseMessaging.unsubscribeFromTopic(FcmTopics.FCM_ALL_TOPIC) }
    }
}
