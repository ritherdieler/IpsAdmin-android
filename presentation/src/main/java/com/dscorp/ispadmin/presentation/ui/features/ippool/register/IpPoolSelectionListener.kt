package com.dscorp.ispadmin.presentation.ui.features.ippool.register

import com.dscorp.ispadmin.domain.model.IpPool

interface IpPoolSelectionListener {

    fun onIpPoolSelected(ipPool: IpPool)
}