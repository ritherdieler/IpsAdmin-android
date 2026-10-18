package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.CustomerData
import com.dscorp.ispadmin.domain.model.CustomerDetail
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.domain.model.extensions.isAValidName
import com.dscorp.ispadmin.domain.model.extensions.isValidDni
import com.dscorp.ispadmin.domain.model.extensions.isValidEmail
import com.dscorp.ispadmin.domain.model.extensions.isValidPhone
import com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository
import com.dscorp.ispadmin.presentation.extension.removeAccents
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.planLabel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface EditHolderGateway {
    suspend fun subscription(subscriptionId: Int): SubscriptionResponse
    suspend fun customer(customerId: Int): CustomerDetail
    suspend fun places(): List<Place>
    suspend fun save(data: CustomerData)
}

class ApiEditHolderGateway(private val repository: IRepository, private val actions: SubscriptionActionsRepository) : EditHolderGateway {
    override suspend fun subscription(subscriptionId: Int) = repository.subscriptionById(subscriptionId)
    override suspend fun customer(customerId: Int) = actions.getCustomer(customerId)
    override suspend fun places() = repository.getPlaces()
    override suspend fun save(data: CustomerData) = repository.updateCustomerData(data)
}

enum class HolderField(val label: String) {
    NAME("Nombres *"), LAST_NAME("Apellidos *"), DNI("DNI *"), PHONE("Teléfono *"), EMAIL("Email"), ADDRESS("Dirección *"), PLACE("Lugar *"),
}

data class HolderForm(
    val name: String,
    val lastName: String,
    val dni: String,
    val phone: String,
    val email: String,
    val address: String,
    val placeId: Int?,
    val placeName: String,
) {
    fun value(field: HolderField): String = when (field) {
        HolderField.NAME -> name
        HolderField.LAST_NAME -> lastName
        HolderField.DNI -> dni
        HolderField.PHONE -> phone
        HolderField.EMAIL -> email
        HolderField.ADDRESS -> address
        HolderField.PLACE -> placeName
    }

    /** Same rules the Core and the old form use; names are stored upper case without accents. */
    fun error(field: HolderField): String? = when (field) {
        HolderField.NAME -> "Nombre inválido".takeUnless { name.isAValidName() }
        HolderField.LAST_NAME -> "Apellido inválido".takeUnless { lastName.isAValidName() }
        HolderField.DNI -> "DNI requiere 8 dígitos".takeUnless { dni.isValidDni(strictValidation = true) }
        HolderField.PHONE -> "Teléfono requiere 9 dígitos".takeUnless { phone.isValidPhone() }
        HolderField.EMAIL -> "Email inválido".takeUnless { email.isEmpty() || email.isValidEmail() }
        HolderField.ADDRESS -> "Escribe la dirección".takeUnless { address.isNotBlank() }
        HolderField.PLACE -> "Elige el lugar".takeUnless { placeId != null }
    }

    fun with(field: HolderField, raw: String): HolderForm = when (field) {
        HolderField.NAME -> copy(name = raw.removeAccents().uppercase())
        HolderField.LAST_NAME -> copy(lastName = raw.removeAccents().uppercase())
        HolderField.DNI -> copy(dni = raw.filter(Char::isDigit).take(DNI_LENGTH))
        HolderField.PHONE -> copy(phone = raw.filter(Char::isDigit).take(PHONE_LENGTH))
        HolderField.EMAIL -> copy(email = raw.trim())
        HolderField.ADDRESS -> copy(address = raw.take(MAX_ADDRESS))
        HolderField.PLACE -> this
    }

    companion object {
        const val MAX_ADDRESS = 300
        private const val DNI_LENGTH = 8
        private const val PHONE_LENGTH = 9
    }
}

data class EditHolderUiState(
    val subscriptionId: Int = 0,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val form: HolderForm? = null,
    val original: HolderForm? = null,
    /** Errors of the stored data and of the fields edited since. */
    val errors: Map<HolderField, String> = emptyMap(),
    val sharedNote: String? = null,
    val places: List<Pair<Int, String>> = emptyList(),
    val done: String? = null,
) {
    val changed: Boolean get() = form != null && form != original
    val canSave: Boolean get() = changed && !saving && form != null && HolderField.entries.none { form.error(it) != null }
}

sealed interface EditHolderIntent {
    data class Load(val subscriptionId: Int) : EditHolderIntent
    data class Changed(val field: HolderField, val value: String) : EditHolderIntent
    data class PlaceSelected(val placeId: Int) : EditHolderIntent
    data object Submit : EditHolderIntent
}

/** a-sus-14 «Editar titular». */
class EditHolderViewModel(private val gateway: EditHolderGateway, private val mainImmediate: CoroutineDispatcher) : ViewModel() {
    private val _uiState = MutableStateFlow(EditHolderUiState())
    val uiState: StateFlow<EditHolderUiState> = _uiState

    fun onIntent(intent: EditHolderIntent) {
        when (intent) {
            is EditHolderIntent.Load -> load(intent.subscriptionId)
            is EditHolderIntent.Changed -> edit(intent.field) { it.with(intent.field, intent.value) }
            is EditHolderIntent.PlaceSelected -> {
                val name = _uiState.value.places.firstOrNull { it.first == intent.placeId }?.second ?: return
                edit(HolderField.PLACE) { it.copy(placeId = intent.placeId, placeName = name) }
            }
            EditHolderIntent.Submit -> submit()
        }
    }

    private fun load(subscriptionId: Int) {
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(subscriptionId = subscriptionId, loading = true, error = null) }
            try {
                val subscription = gateway.subscription(subscriptionId)
                val places = runCatching { gateway.places() }.getOrDefault(emptyList())
                    .mapNotNull { place -> place.id?.toIntOrNull()?.let { it to place.name.orEmpty().toTitleCase() } }
                val services = subscription.customerId?.let { id -> runCatching { gateway.customer(id) }.getOrNull()?.subscriptions }.orEmpty()
                val form = HolderForm(
                    name = subscription.firstName.orEmpty().removeAccents().uppercase(),
                    lastName = subscription.lastName.orEmpty().removeAccents().uppercase(),
                    dni = subscription.dni.orEmpty(),
                    phone = subscription.phone.orEmpty(),
                    email = subscription.email.orEmpty(),
                    address = subscription.address.orEmpty(),
                    placeId = subscription.place?.id?.toIntOrNull(),
                    placeName = subscription.place?.name.orEmpty().toTitleCase(),
                )
                _uiState.update {
                    it.copy(
                        loading = false, form = form, original = form, places = places, sharedNote = sharedNote(services),
                        // Stored data may already break a rule (e.g. a 7-digit DNI): say so instead of silently blocking «Guardar».
                        errors = HolderField.entries.mapNotNull { field -> form.error(field)?.let { field to it } }.toMap(),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, error = LOAD_ERROR) }
            }
        }
    }

    private fun edit(field: HolderField, change: (HolderForm) -> HolderForm) {
        _uiState.update { state ->
            val form = state.form?.let(change) ?: return@update state
            val errors = state.errors - field + (form.error(field)?.let { mapOf(field to it) } ?: emptyMap())
            state.copy(form = form, errors = errors, error = null)
        }
    }

    private fun submit() {
        val state = _uiState.value
        val form = state.form ?: return
        val errors = HolderField.entries.mapNotNull { field -> form.error(field)?.let { field to it } }.toMap()
        if (errors.isNotEmpty() || !state.changed || state.saving) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(saving = true, error = null) }
            try {
                gateway.save(
                    CustomerData(
                        subscriptionId = state.subscriptionId,
                        name = form.name,
                        lastName = form.lastName,
                        dni = form.dni,
                        place = form.placeName,
                        address = form.address.trim(),
                        phone = form.phone,
                        email = form.email,
                        placeId = form.placeId,
                    ),
                )
                _uiState.update { it.copy(saving = false, done = "Datos del titular actualizados") }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(saving = false, error = SAVE_ERROR) }
            }
        }
    }

    private companion object {
        const val LOAD_ERROR = "No se pudo cargar el titular. Revisa tu conexión y vuelve a intentar."
        const val SAVE_ERROR = "No se pudieron guardar los datos. Revisa tu conexión y vuelve a intentar."

        fun sharedNote(services: List<com.dscorp.ispadmin.domain.model.CustomerServiceSummary>): String? {
            if (services.size < 2) return null
            val labels = services.map { service ->
                val type = when (service.installationType) {
                    "FIBER" -> "Fibra"
                    "WIRELESS" -> "Inalámbrico"
                    "ONLY_TV_FIBER" -> "Solo TV"
                    else -> null
                }
                listOfNotNull(type, service.planName?.takeIf { it.isNotBlank() }?.let(::planLabel)).joinToString(" · ").ifBlank { "Cód. ${service.id}" }
            }
            return "Se aplica a los ${services.size} servicios de este titular: ${labels.dropLast(1).joinToString(", ")} y ${labels.last()}."
        }
    }
}
