package com.dscorp.ispadmin.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanResponseTest {

    private fun plan(id: String?, name: String? = "Plan", price: Double? = 10.0) =
        PlanResponse(id = id, name = name, price = price, type = InstallationType.FIBER)

    @Test
    fun `equals does not throw with null or another type`() {
        val plan = plan("p1")

        assertFalse(plan.equals(null))
        assertFalse(plan.equals("x"))
    }

    @Test
    fun `plans with the same id are equal and share the hash code`() {
        val a = plan("p1", name = "A", price = 10.0)
        val b = plan("p1", name = "B", price = 99.0)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertEquals(1, setOf(a, b).size)
    }

    @Test
    fun `plans with different ids are not equal`() {
        assertFalse(plan("p1") == plan("p2"))
    }

    @Test
    fun `a plan without id hashes without throwing and equals another without id`() {
        val a = plan(null)
        val b = plan(null, name = "Other")

        assertTrue(a == b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
