package com.dscorp.ispadmin.presentation.ui.features.napboxeslist

import android.view.View
import com.dscorp.ispadmin.domain.model.NapBoxResponse

interface OnItemClickListener {
    fun onItemClick(napBox: NapBoxResponse)
    fun onNapBoxPopupButtonSelected(napbox: NapBoxResponse, view: View)

}
