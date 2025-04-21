package com.dscorp.ispadmin.presentation.ui.features.ippool.seeip

import com.dscorp.ispadmin.domain.model.Ip

sealed class IpListUiState {
    class IpListReady(val ips: List<Ip>) : IpListUiState()

}
