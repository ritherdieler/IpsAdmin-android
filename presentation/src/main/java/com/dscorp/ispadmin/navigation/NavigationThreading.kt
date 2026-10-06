package com.dscorp.ispadmin.navigation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun <T> runNavigationOnMainThread(navigate: () -> T): T =
    withContext(Dispatchers.Main.immediate) {
        navigate()
    }
