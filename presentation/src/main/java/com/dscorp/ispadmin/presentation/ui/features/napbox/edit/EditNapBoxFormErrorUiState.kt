package com.dscorp.ispadmin.presentation.ui.features.napbox.edit

/**
 * Created by Sergio Carrillo Diestra on 13/12/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
sealed class EditNapBoxFormErrorUiState(val error: String? = null) {
    companion object {
        const val CODE_ERROR = "El codigo no puede estar vacio"
        const val ADDRESS_ERROR = "La dirección no puede estar vacio"
        const val LOCATION_ERROR = "La ubicación no puede estar vacio"
    }

    class OnEtCodeError : EditNapBoxFormErrorUiState(CODE_ERROR)
    class OnEtAddressError : EditNapBoxFormErrorUiState(ADDRESS_ERROR)
    class OnEtLocationError : EditNapBoxFormErrorUiState(LOCATION_ERROR)

    object OnEtCodeCleanError : EditNapBoxFormErrorUiState()
    object OnEtAddressCleanError : EditNapBoxFormErrorUiState()
    object OnEtLocationCleanError : EditNapBoxFormErrorUiState()
}
