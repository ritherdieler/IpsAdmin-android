package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.data.datasource.remote.WispApiService
import com.dscorp.ispadmin.data.utils.mapToDomain
import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.repository.PaymentRepository
import retrofit2.HttpException
import java.io.IOException

class PaymentRepositoryImpl(
    private val apiService: WispApiService
) : PaymentRepository {
    
    override suspend fun getPaymentById(paymentId: String): Payment {
        try {
            val response = apiService.getPaymentById(paymentId)
            if (response.isSuccessful && response.body() != null) {
                return response.body()!!.mapToDomain()
            } else {
                throw HttpException(response)
            }
        } catch (e: IOException) {
            throw IOException("Error de conexión al obtener el pago", e)
        } catch (e: HttpException) {
            throw IOException("Error al obtener el pago: ${e.code()}", e)
        } catch (e: Exception) {
            throw IOException("Error desconocido al obtener el pago", e)
        }
    }
} 