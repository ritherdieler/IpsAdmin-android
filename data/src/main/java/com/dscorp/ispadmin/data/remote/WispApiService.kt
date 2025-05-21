package com.dscorp.ispadmin.data.remote

import com.dscorp.ispadmin.domain.model.Payment
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface WispApiService {
    @GET("payment/{id}")
    suspend fun getPaymentById(@Path("id") paymentId: String): Response<Payment>
} 