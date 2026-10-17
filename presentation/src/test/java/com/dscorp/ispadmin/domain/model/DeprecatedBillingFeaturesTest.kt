package com.dscorp.ispadmin.domain.model

import com.dscorp.ispadmin.presentation.ui.features.report.DownloadDocumentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.reflect.full.memberProperties

/** Payment commitments and coupons were removed from the Core; the app must not model them. */
class DeprecatedBillingFeaturesTest {

    @Test
    fun `subscription models carry no commitment or coupon fields`() {
        val deprecated = setOf("paymentCommitmentDate", "isPaymentCommitment", "coupon")
        assertEquals(emptyList<String>(), SubscriptionResponse::class.memberProperties.map { it.name }.filter { it in deprecated })
        assertEquals(emptyList<String>(), Subscription::class.memberProperties.map { it.name }.filter { it in deprecated })
    }

    @Test
    fun `payment commitment report is gone`() {
        assertEquals(emptyList<String>(), DownloadDocumentType.entries.map { it.name }.filter { it == "WITH_PAYMENT_COMMITMENT" || it == "SUSPENDED" })
    }

    @Test
    fun `coupon model is gone`() {
        assertNull(runCatching { Class.forName("com.dscorp.ispadmin.domain.model.Coupon") }.getOrNull())
    }
}
