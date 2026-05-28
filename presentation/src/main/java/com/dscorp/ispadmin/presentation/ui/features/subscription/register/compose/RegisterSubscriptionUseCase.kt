package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.data.repository.InstallationOrderRepository
import com.dscorp.ispadmin.domain.model.Subscription
import java.io.File

class RegisterSubscriptionUseCase(
    private val repository: IRepository,
    private val installationOrderRepository: InstallationOrderRepository
) {

    // Registra una suscripcion nueva.
    // Si recibe foto de fachada, usa el nuevo endpoint multipart.
    // Si no recibe foto, mantiene el flujo normal existente.
    suspend operator fun invoke(
        subscription: Subscription,
        orderId: Int?,
        facadePhotoFile: File? = null
    ): Result<Subscription> {
        return try {
            val registeredSubscription = if (facadePhotoFile != null) {
                // Registra la suscripcion enviando tambien la foto de fachada.
                repository.registerSubscriptionWithFacadePhoto(
                    subscription = subscription,
                    facadePhotoFile = facadePhotoFile
                )
            } else {
                // Mantiene el registro anterior sin foto.
                repository.registerSubscription(subscription)
            }

            orderId?.let {
                // Cierra la orden de instalacion asociada cuando el registro termina correctamente.
                installationOrderRepository.closeInstallationOrder(it)
            }

            Result.success(registeredSubscription)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}