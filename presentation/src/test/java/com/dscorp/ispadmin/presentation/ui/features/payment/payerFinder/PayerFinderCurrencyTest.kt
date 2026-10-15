package com.dscorp.ispadmin.presentation.ui.features.payment.payerFinder

import org.junit.Assert.assertTrue
import org.junit.Test

class PayerFinderCurrencyTest {

    @Test
    fun `amounts are formatted in peruvian soles`() {
        val formatted = formatCurrency(1234.5)

        assertTrue(formatted, formatted.startsWith("S/"))
        assertTrue(formatted, formatted.endsWith("1,234.50"))
    }
}
