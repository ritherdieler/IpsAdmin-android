package com.dscorp.ispadmin.presentation.ui.features.dashboard

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.designsystem.components.HeroTitleBar
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

private const val MINUTES_SHOWN_AS_RELATIVE = 60

/** Navigation the dashboard needs from the app shell. Null actions hide their controls (previews and tests). */
data class DashboardActions(
    val openTickets: (() -> Unit)? = null,
    val openSubscriptions: (() -> Unit)? = null,
)

val LocalDashboardActions = staticCompositionLocalOf { DashboardActions() }

/** Title row of every dashboard tab: "Dashboard", status line and the tab's actions. */
@Composable
fun DashboardTitleBar(status: String?, statusTag: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    HeroTitleBar(stringResource(R.string.dashboard_screen_title), status, onMenu = null,
        subtitleTag = statusTag, actions = actions)
}

/** "Actualizado hace 3 min"; the clock time once it is older than an hour. */
@Composable
fun updatedLabel(millis: Long?, now: Long = System.currentTimeMillis()): String? {
    millis ?: return null
    val minutes = TimeUnit.MILLISECONDS.toMinutes((now - millis).coerceAtLeast(0)).toInt()
    return when {
        minutes < 1 -> stringResource(R.string.dashboard_updated_now)
        minutes < MINUTES_SHOWN_AS_RELATIVE -> stringResource(R.string.dashboard_updated_minutes, minutes)
        else -> stringResource(R.string.dashboard_updated_at, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis)))
    }
}
