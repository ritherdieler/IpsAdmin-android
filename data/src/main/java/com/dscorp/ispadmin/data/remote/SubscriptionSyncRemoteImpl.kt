package com.dscorp.ispadmin.data.remote

import com.dscorp.ispadmin.domain.repository.SubscriptionSyncOutcome
import com.dscorp.ispadmin.domain.repository.SubscriptionSyncRemote
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

class SubscriptionSyncRemoteImpl(
    private val api: PendingSubscriptionSyncApi,
    private val gson: Gson = Gson(),
    private val pollIntervalMs: Long = 2_000L,
    private val pollTimeoutMs: Long = 120_000L,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : SubscriptionSyncRemote {

    override suspend fun uploadPending(
        subscriptionJson: String,
        clientRequestId: String,
        installationOrderId: Int?,
        facadePhotoFile: File?
    ): SubscriptionSyncOutcome {
        return try {
            val payload = mergeIdentity(subscriptionJson, clientRequestId, installationOrderId)
            val subscriptionBody = payload.toRequestBody("application/json".toMediaTypeOrNull())
            val photoFile = facadePhotoFile ?: return SubscriptionSyncOutcome.Failure("Falta foto de fachada")
            val photoPart = MultipartBody.Part.createFormData(
                name = "facadePhoto",
                filename = photoFile.name,
                body = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            val response = api.registerWithFacadePhoto(subscriptionBody, photoPart)
            when (val mapped = mapResponse(response)) {
                SubscriptionSyncOutcome.Success -> {
                    val subscriptionId = response.body()?.data?.id
                    if (subscriptionId != null && response.body()?.data?.provisioningPending == true) {
                        awaitRegistrationDone(subscriptionId)
                    } else {
                        mapped
                    }
                }
                else -> mapped
            }
        } catch (error: IOException) {
            SubscriptionSyncOutcome.Failure(error.message ?: "Error de red")
        }
    }

    private suspend fun awaitRegistrationDone(subscriptionId: Int): SubscriptionSyncOutcome {
        val deadline = clock() + pollTimeoutMs
        var lastError: String? = null
        while (clock() <= deadline) {
            try {
                val response = api.getRegistrationProgress(subscriptionId)
                if (response.code() in 200..299) {
                    val body = response.body()
                    if (body?.done == true) {
                        return SubscriptionSyncOutcome.Success
                    }
                } else {
                    lastError = "HTTP ${response.code()}"
                }
            } catch (error: IOException) {
                lastError = error.message
            }
            delay(pollIntervalMs)
        }
        return SubscriptionSyncOutcome.Failure(
            lastError ?: "Timeout esperando aprovisionamiento de la suscripción $subscriptionId"
        )
    }

    private fun mapResponse(
        response: retrofit2.Response<SubscriptionSyncApiResponse>
    ): SubscriptionSyncOutcome {
        val body = response.body() ?: parseErrorBody(response)
        val status = body?.status ?: response.code()
        return when {
            status == 200 || status == 201 -> SubscriptionSyncOutcome.Success
            status == 409 && body?.errorCode == IP_CONFLICT_CODE -> SubscriptionSyncOutcome.IpConflict
            status == 409 -> SubscriptionSyncOutcome.Conflict
            else -> SubscriptionSyncOutcome.Failure(
                body?.error ?: "HTTP ${response.code()}"
            )
        }
    }

    private fun parseErrorBody(
        response: retrofit2.Response<SubscriptionSyncApiResponse>
    ): SubscriptionSyncApiResponse? {
        val raw = response.errorBody()?.string() ?: return null
        return runCatching { gson.fromJson(raw, SubscriptionSyncApiResponse::class.java) }.getOrNull()
    }

    private fun mergeIdentity(
        subscriptionJson: String,
        clientRequestId: String,
        installationOrderId: Int?
    ): String {
        val json = JsonParser.parseString(subscriptionJson).asJsonObject
        json.addProperty("clientRequestId", clientRequestId)
        if (installationOrderId != null) {
            json.addProperty("installationOrderId", installationOrderId)
        }
        return json.toString()
    }

    private companion object {
        const val IP_CONFLICT_CODE = "IP_CONFLICT"
    }
}
