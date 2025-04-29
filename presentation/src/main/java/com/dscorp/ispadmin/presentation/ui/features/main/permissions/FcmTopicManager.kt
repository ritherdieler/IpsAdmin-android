package com.dscorp.ispadmin.presentation.ui.features.main.permissions

import com.dscorp.ispadmin.domain.model.User.UserType
import com.dscorp.ispadmin.presentation.fcm.FcmTopics
import com.google.firebase.messaging.FirebaseMessaging

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
        // Primero aplicamos las suscripciones comunes para todos los usuarios
        subscribeToCommonTopics()
        
        // Luego aplicamos las suscripciones específicas según el tipo de usuario
        when (userType) {
            UserType.TECHNICIAN -> subscribeTechnicianTopics()
            UserType.SECRETARY, UserType.ACCOUNTANT -> subscribeSecretaryAndAccountantTopics()
            UserType.ADMIN -> subscribeAdminTopics()
            UserType.SALES -> subscribeSalesTopics()
            else -> { /* No hay suscripciones especiales para otros tipos */ }
        }
    }
    
    private fun subscribeToCommonTopics() {
        // Tópicos a los que todos los usuarios deben estar suscritos
        firebaseMessaging.subscribeToTopic(FcmTopics.FCM_ALL_TOPIC)
    }
    
    private fun subscribeTechnicianTopics() {
        firebaseMessaging.subscribeToTopic(FcmTopics.FCM_TECHNICIAN_TOPIC)
        firebaseMessaging.subscribeToTopic(FcmTopics.ASSISTANCE_TICKET)
    }
    
    private fun subscribeSecretaryAndAccountantTopics() {
        firebaseMessaging.subscribeToTopic(FcmTopics.FCM_SECRETARY_TOPIC)
        firebaseMessaging.subscribeToTopic(FcmTopics.ASSISTANCE_TICKET_ADMINS)
        firebaseMessaging.subscribeToTopic(FcmTopics.ASSISTANCE_TICKET)
        firebaseMessaging.subscribeToTopic(FcmTopics.TOPIC_INSTALLATION_ORDER)
    }
    
    private fun subscribeAdminTopics() {
        firebaseMessaging.subscribeToTopic(FcmTopics.ASSISTANCE_TICKET_ADMINS)
        firebaseMessaging.subscribeToTopic(FcmTopics.ASSISTANCE_TICKET)
        firebaseMessaging.subscribeToTopic(FcmTopics.TOPIC_INSTALLATION_ORDER)
    }
    
    private fun subscribeSalesTopics() {
        firebaseMessaging.subscribeToTopic(FcmTopics.TOPIC_INSTALLATION_ORDER)
    }
} 