package com.dscorp.ispadmin.presentation.navigation

import com.dscorp.ispadmin.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Test

class DrawerNavigationRolesTest {

    @Test
    fun `every user type gets a drawer without crashing`() {
        User.UserType.entries.forEach { type ->
            val groups = DrawerNavigation.getDrawerGroupsForUser(type)
            assert(groups.isNotEmpty()) { "$type has no drawer groups" }
        }
    }

    @Test
    fun `logistic and client users only get the profile`() {
        listOf(User.UserType.LOGISTIC, User.UserType.CLIENT).forEach { type ->
            assertEquals(type.name, listOf(DrawerGroup.Profile), DrawerNavigation.getDrawerGroupsForUser(type))
        }
    }
}
