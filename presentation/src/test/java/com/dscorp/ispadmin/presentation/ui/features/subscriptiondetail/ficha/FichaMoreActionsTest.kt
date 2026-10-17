package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.subscription
import org.junit.Assert.assertEquals
import org.junit.Test

class FichaMoreActionsTest {

    @Test
    fun `a cut fiber service with a holder offers the design groups`() {
        val sections = moreActions(subscription(status = ServiceStatus.CUT_OFF).copy(customerId = 7))

        assertEquals(
            listOf(
                MoreSection("Servicio", listOf(MoreAction.CHANGE_PLAN, MoreAction.CHANGE_NAP, MoreAction.UPDATE_LOCATION, MoreAction.NEW_SERVICE)),
                MoreSection("Cobranza", listOf(MoreAction.DEFER_CUT)),
                MoreSection("Titular", listOf(MoreAction.EDIT_HOLDER, MoreAction.UPDATE_PHOTO)),
                MoreSection(null, listOf(MoreAction.CANCEL)),
            ),
            sections,
        )
        assertEquals(
            listOf("Cambiar plan", "Cambiar caja NAP", "Actualizar ubicación", "Nuevo servicio para el titular", "Postergar corte",
                "Editar datos del titular", "Actualizar foto de fachada", "Cancelar suscripción"),
            sections.flatMap { it.actions }.map { it.label },
        )
    }

    @Test
    fun `wireless services migrate instead of changing NAP and a cancelled one can only be reactivated or edited`() {
        val wireless = moreActions(subscription(status = ServiceStatus.ACTIVE).copy(installationType = InstallationType.WIRELESS))
        assertEquals(
            listOf(MoreAction.CHANGE_PLAN, MoreAction.MIGRATE_TO_FIBER, MoreAction.UPDATE_LOCATION),
            wireless.first().actions,
        )

        val cancelled = moreActions(subscription(status = ServiceStatus.CANCELLED))
        assertEquals(
            listOf(
                MoreSection("Servicio", listOf(MoreAction.UPDATE_LOCATION)),
                MoreSection("Titular", listOf(MoreAction.EDIT_HOLDER, MoreAction.UPDATE_PHOTO)),
                MoreSection(null, listOf(MoreAction.REACTIVATE)),
            ),
            cancelled,
        )
    }
}
