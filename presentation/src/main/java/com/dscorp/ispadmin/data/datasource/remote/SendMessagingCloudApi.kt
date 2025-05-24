package com.dscorp.ispadmin.data.datasource.remote

import com.dscorp.ispadmin.BuildConfig
import com.dscorp.ispadmin.domain.model.FireBaseResponse
import com.dscorp.ispadmin.domain.model.FirebaseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface SendMessagingCloudApi {
    @Headers(
        "Content-Type:application/json",
        "Authorization:key=${BuildConfig.SEND_CLOUD_MESSAGING_API_KEY}"
    )
    @POST("fcm/send")
   suspend fun sendCloudMessaging(@Body body: FirebaseBody?):Response<FireBaseResponse>
}