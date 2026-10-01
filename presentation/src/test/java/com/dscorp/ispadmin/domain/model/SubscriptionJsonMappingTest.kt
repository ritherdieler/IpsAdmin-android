package com.dscorp.ispadmin.domain.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubscriptionJsonMappingTest {

    private val gson = Gson()

    @Test
    fun `deserializes backend id into subscriptionId`() {
        val json = """{"id":2288,"tr069ProvisionStatus":"MANUAL_REQUIRED","firstName":"Test"}"""

        val subscription = gson.fromJson(json, Subscription::class.java)

        assertEquals(2288, subscription.subscriptionId)
    }

    @Test
    fun `deserializes pppoe password from registration response`() {
        val json = """{"pppoePassword":"demo-pppoe-password"}"""

        val subscription = gson.fromJson(json, Subscription::class.java)

        assertEquals(
            "demo-pppoe-password",
            gson.toJsonTree(subscription).asJsonObject.get("pppoePassword").asString,
        )
    }

    @Test
    fun `serializes subscriptionId as id for backend requests`() {
        val subscription = Subscription(subscriptionId = 99, firstName = "Ana")

        val json = gson.toJson(subscription)

        assertEquals(true, json.contains("\"id\":99"))
        assertEquals(false, json.contains("subscriptionId"))
    }

    @Test
    fun `resolvedSubscriptionId returns subscriptionId when present`() {
        assertEquals(42, Subscription(subscriptionId = 42).resolvedSubscriptionId())
    }

    @Test
    fun `resolvedSubscriptionId returns null when missing`() {
        assertNull(Subscription().resolvedSubscriptionId())
    }
}
