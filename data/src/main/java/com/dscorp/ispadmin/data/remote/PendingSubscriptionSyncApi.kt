package com.dscorp.ispadmin.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface PendingSubscriptionSyncApi {
    @Multipart
    @POST("subscription/with-facade-photo")
    suspend fun registerWithFacadePhoto(
        @Part("subscription") subscription: RequestBody,
        @Part facadePhoto: MultipartBody.Part
    ): Response<SubscriptionSyncApiResponse>

    @GET("subscription/{subscriptionId}/registration-progress")
    suspend fun getRegistrationProgress(
        @Path("subscriptionId") subscriptionId: Int
    ): Response<RegistrationProgressDto>
}
