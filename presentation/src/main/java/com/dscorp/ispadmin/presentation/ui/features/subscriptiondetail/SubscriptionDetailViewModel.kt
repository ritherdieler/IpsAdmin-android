package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.observability.ObsBreadcrumbCategory
import com.dscorp.ispadmin.observability.ObservabilityClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SubscriptionDetailViewModel(
    val repository: IRepository,
    private val observabilityClient: ObservabilityClient,
    private val subscriptionActionsRepository: com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository? = null,
) : ViewModel() {

    private companion object {
        const val OBS_FEATURE = "subscription"
        const val OBS_SCREEN = "subscription_detail"
    }

    val uiState = MutableStateFlow(SubscriptionDetailUiState())

    fun getSubscription(subscriptionId: Int) = viewModelScope.launch {
        observabilityClient.addBreadcrumb(
            category = ObsBreadcrumbCategory.NAVIGATION,
            message = "$OBS_FEATURE.load_detail",
            data = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "entityId" to subscriptionId)
        )
        try {
            uiState.value = uiState.value.copy(isLoading = true)
            val subscriptionResponse = repository.subscriptionById(subscriptionId)
            val otherServices = subscriptionResponse.customerId
                ?.let { customerId -> subscriptionActionsRepository?.getCustomer(customerId) }
                ?.subscriptions
                .orEmpty()
                .filter { it.id != subscriptionId }
            uiState.value = uiState.value.copy(
                subscription = subscriptionResponse,
                otherServices = otherServices,
                isLoading = false
            )
        } catch (e: Exception) {
            observabilityClient.reportError(
                throwable = e,
                message = "Fallo al cargar detalle de suscripción",
                tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "load_detail", "entityId" to subscriptionId)
            )
            uiState.value =
                uiState.value.copy(error = e.message, isLoading = false, subscription = null)
        }
    }

    // Actualiza la foto de fachada de la suscripcion actual.
// La imagen se envia al backend; el backend la sube a Firebase y devuelve la URL guardada.
    fun updateFacadePhoto(
        subscriptionId: Int,
        facadePhotoFile: File
    ) = viewModelScope.launch {
        observabilityClient.addBreadcrumb(
            category = ObsBreadcrumbCategory.USER_ACTION,
            message = "$OBS_FEATURE.update_facade_photo",
            data = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "entityId" to subscriptionId)
        )
        try {
            uiState.value = uiState.value.copy(isLoading = true)

            val updatedSubscription = repository.updateSubscriptionFacadePhoto(
                subscriptionId = subscriptionId,
                facadePhotoFile = facadePhotoFile
            )

            uiState.value = uiState.value.copy(
                subscription = uiState.value.subscription?.copy(
                    facadePhotoUrl = updatedSubscription.facadePhotoUrl
                ),
                isLoading = false,
                error = null
            )
            observabilityClient.addBreadcrumb(
                category = ObsBreadcrumbCategory.STATE,
                message = "$OBS_FEATURE.update_facade_photo.success",
                data = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "entityId" to subscriptionId)
            )
        } catch (e: Exception) {
            observabilityClient.reportError(
                throwable = e,
                message = "Fallo al actualizar foto de fachada",
                tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "update_facade_photo", "entityId" to subscriptionId)
            )
            uiState.value = uiState.value.copy(
                isLoading = false,
                error = e.message ?: "No se pudo actualizar la foto de fachada"
            )
        }
    }



    fun clearError() {
        uiState.value = uiState.value.copy(error = null)
    }

    data class SubscriptionDetailUiState(
        val subscription: SubscriptionResponse? = null,
        val otherServices: List<com.dscorp.ispadmin.domain.model.CustomerServiceSummary> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )


}
