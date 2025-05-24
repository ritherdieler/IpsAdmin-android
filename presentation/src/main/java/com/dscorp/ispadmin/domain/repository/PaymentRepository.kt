package com.dscorp.ispadmin.domain.repository

import com.dscorp.ispadmin.domain.model.Payment

interface PaymentRepository {
    suspend fun getPaymentById(paymentId: String): Payment
    
    // Otros métodos existentes o futuros
} 