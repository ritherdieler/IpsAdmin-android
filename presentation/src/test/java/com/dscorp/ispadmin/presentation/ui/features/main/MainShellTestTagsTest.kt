package com.dscorp.ispadmin.presentation.ui.features.main

import com.dscorp.ispadmin.navigation.MainDestination
import com.dscorp.ispadmin.navigation.MoreItem
import com.dscorp.ispadmin.presentation.ui.features.more.MoreTestTags
import org.junit.Assert.assertEquals
import org.junit.Test

/** Replaces MainNavTestTagsTest: instrumented e2e flows and journeys rely on these tags. */
class MainShellTestTagsTest {

    @Test
    fun `shell tags are stable`() {
        assertEquals("main_bottom_bar", MainShellTestTags.BOTTOM_BAR)
        assertEquals("main_nav_subscriptions", MainShellTestTags.item(MainDestination.SUBSCRIPTIONS))
        assertEquals("main_nav_more", MainShellTestTags.item(MainDestination.MORE))
        assertEquals("more_item_register_subscription", MoreTestTags.item(MoreItem.REGISTER_SUBSCRIPTION))
    }
}
