package com.dscorp.ispadmin.presentation.ui.features.napbox.edit

import com.dscorp.ispadmin.domain.model.NapBoxResponse

/**
 * Created by Sergio Carrillo Diestra on 13/12/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
sealed class EditNapBoxUiState {

    class FetchFormDataError(val error: String) : EditNapBoxUiState()

    class EditNapBoxSuccess(val napBox: NapBoxResponse) : EditNapBoxUiState()
    class EditNapBoxError(val error: String?) : EditNapBoxUiState()
}
