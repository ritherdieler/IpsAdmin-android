package com.dscorp.ispadmin.data.repository.adapters

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.repository.UserSessionReader

class UserSessionReaderAdapter(
    private val repository: IRepository
) : UserSessionReader {

    override fun getUserSession() = repository.getUserSession()
}
