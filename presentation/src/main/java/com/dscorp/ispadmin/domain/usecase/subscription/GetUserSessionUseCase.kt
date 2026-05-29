package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.repository.UserSessionReader

class GetUserSessionUseCase(private val userSessionReader: UserSessionReader) {
    operator fun invoke(): Result<User?> = runCatching {
        userSessionReader.getUserSession()
    }
}
