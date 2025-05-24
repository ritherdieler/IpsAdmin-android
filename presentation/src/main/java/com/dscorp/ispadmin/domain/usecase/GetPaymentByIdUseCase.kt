package com.dscorp.ispadmin.domain.usecase

import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.repository.PaymentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetPaymentByIdUseCase(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(paymentId: String): Result<Payment> = runCatching {
        withContext(Dispatchers.IO) {
            paymentRepository.getPaymentById(paymentId)
        }
    }
} 