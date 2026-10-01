package com.dscorp.ispadmin.domain.model

class RegistrationConflictException(
    val errorCode: String?,
    message: String,
) : Exception(message)
