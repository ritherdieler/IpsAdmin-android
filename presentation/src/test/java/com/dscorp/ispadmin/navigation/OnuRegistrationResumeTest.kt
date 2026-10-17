package com.dscorp.ispadmin.navigation

import com.dscorp.ispadmin.domain.model.OnuRegistrationOperation
import com.dscorp.ispadmin.domain.model.User
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnuRegistrationResumeTest {

    @Test
    fun `does not open the register form when there is no active operation`() {
        assertFalse(shouldResumeOnuRegistration(user(User.UserType.ADMIN), activeOperation = null))
        assertFalse(shouldResumeOnuRegistration(user(User.UserType.TECHNICIAN), activeOperation = null))
    }

    @Test
    fun `resumes an unfinished registration for roles that can register`() {
        listOf(User.UserType.ADMIN, User.UserType.ACCOUNTANT, User.UserType.TECHNICIAN).forEach { type ->
            assertTrue(type.name, shouldResumeOnuRegistration(user(type), operation(subscriptionId = null)))
        }
    }

    @Test
    fun `does not resume for roles that cannot register subscriptions`() {
        listOf(User.UserType.SECRETARY, User.UserType.SALES, User.UserType.LOGISTIC, User.UserType.CLIENT)
            .forEach { type ->
                assertFalse(type.name, shouldResumeOnuRegistration(user(type), operation(subscriptionId = null)))
            }
    }

    @Test
    fun `does not resume when the operation already produced a subscription`() {
        assertFalse(shouldResumeOnuRegistration(user(User.UserType.ADMIN), operation(subscriptionId = 42)))
    }

    @Test
    fun `does not resume without a logged user`() {
        assertFalse(shouldResumeOnuRegistration(user = null, activeOperation = operation(subscriptionId = null)))
    }

    private fun user(type: User.UserType) = User(id = 1, name = "Test", lastName = "User", type = type)

    private fun operation(subscriptionId: Int?) = OnuRegistrationOperation(
        id = "op-1",
        serial = "HWTC12345678",
        subscriptionId = subscriptionId,
        phase = "READY_FOR_FORM",
        state = "READY_FOR_FORM",
        revision = 1,
    )
}
