package com.dscorp.ispadmin.presentation.ui.features.main.permissions

import com.dscorp.ispadmin.domain.model.User.UserType
import com.dscorp.ispadmin.presentation.fcm.FcmTopics
import com.google.firebase.messaging.FirebaseMessaging

private val ROLE_TOPICS = setOf(
    FcmTopics.FCM_TECHNICIAN_TOPIC,
    FcmTopics.FCM_SECRETARY_TOPIC,
    FcmTopics.ASSISTANCE_TICKET,
    FcmTopics.ASSISTANCE_TICKET_ADMINS,
    FcmTopics.TOPIC_INSTALLATION_ORDER,
)

/**
 * Gestor de suscripciones a tópicos FCM que centraliza la lógica de
 * suscripción según el tipo de usuario.
 */
class FcmTopicManager(private val firebaseMessaging: FirebaseMessaging) {

    /**
     * Suscribe al usuario a los tópicos FCM correspondientes a su tipo.
     * @param userType el tipo de usuario
     */
    fun subscribeToTopicsForUserType(userType: UserType) {
        val roleTopics = roleTopicsFor(userType)
        firebaseMessaging.subscribeToTopic(FcmTopics.FCM_ALL_TOPIC)
        roleTopics.forEach(firebaseMessaging::subscribeToTopic)
        // Drop topics left by a previous user of another role on this device.
        (ROLE_TOPICS - roleTopics).forEach(firebaseMessaging::unsubscribeFromTopic)
    }

    // Keeps only the device-wide topic once nobody is signed in.
    fun unsubscribeFromRoleTopics() {
        ROLE_TOPICS.forEach(firebaseMessaging::unsubscribeFromTopic)
    }

    private fun roleTopicsFor(userType: UserType): Set<String> = when (userType) {
        UserType.TECHNICIAN -> setOf(FcmTopics.FCM_TECHNICIAN_TOPIC, FcmTopics.ASSISTANCE_TICKET)
        UserType.SECRETARY, UserType.ACCOUNTANT -> setOf(
            FcmTopics.FCM_SECRETARY_TOPIC,
            FcmTopics.ASSISTANCE_TICKET_ADMINS,
            FcmTopics.ASSISTANCE_TICKET,
            FcmTopics.TOPIC_INSTALLATION_ORDER,
        )
        UserType.ADMIN -> setOf(
            FcmTopics.ASSISTANCE_TICKET_ADMINS,
            FcmTopics.ASSISTANCE_TICKET,
            FcmTopics.TOPIC_INSTALLATION_ORDER,
        )
        UserType.SALES -> setOf(FcmTopics.TOPIC_INSTALLATION_ORDER)
        UserType.LOGISTIC, UserType.CLIENT -> emptySet()
    }
}
