package com.dscorp.ispadmin.presentation.ui.features.more

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.dscorp.ispadmin.data.datasource.remote.PlanApi
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.subscription.ObservePendingSubscriptionsUseCase
import com.dscorp.ispadmin.navigation.MainNavigation
import com.dscorp.ispadmin.navigation.MoreItem
import com.dscorp.ispadmin.presentation.ui.features.profile.ProfileViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Wires «Más» to the session user, the offline queue count, the active plans and logout. */
@Composable
fun MoreRoute(user: User, versionLabel: String, onItemSelected: (MoreItem) -> Unit, onProfile: () -> Unit, onLoggedOut: () -> Unit) {
    val type = user.type ?: return
    val observePending: ObservePendingSubscriptionsUseCase = koinInject()
    val planApi: PlanApi = koinInject()
    val profile: ProfileViewModel = koinViewModel()
    val session by profile.uiState.collectAsState()

    val pendingFlow = remember { observePending().getOrNull()?.map { it.size } ?: flowOf(null) }
    val pending by pendingFlow.collectAsState(initial = null)
    val items = remember(type, pending) { MainNavigation.moreItemsFor(type, pending ?: 0) }
    val activePlans by produceState<Int?>(null, items) {
        if (MoreItem.PLANS !in items) return@produceState
        value = try {
            planApi.allWithCounts().count { it.isActive }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
    LaunchedEffect(session.isLoggedOut) { if (session.isLoggedOut) onLoggedOut() }

    MoreContent(
        menu = MoreMenu.from(user, items, pending, activePlans),
        versionLabel = versionLabel,
        onItemSelected = onItemSelected,
        onProfile = onProfile,
        onLogout = profile::logOut,
    )
}
