package com.dscorp.ispadmin.domain.repository

import com.dscorp.ispadmin.domain.model.User

interface UserSessionReader {
    fun getUserSession(): User?
}
