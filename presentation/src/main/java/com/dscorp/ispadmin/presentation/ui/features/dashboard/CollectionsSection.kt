package com.dscorp.ispadmin.presentation.ui.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.dashboard.CollectionSummary
import com.dscorp.ispadmin.presentation.designsystem.components.BadgeTone
import com.dscorp.ispadmin.presentation.designsystem.components.BarItem
import com.dscorp.ispadmin.presentation.designsystem.components.HorizontalBarList
import com.dscorp.ispadmin.presentation.designsystem.components.IspCard
import com.dscorp.ispadmin.presentation.designsystem.components.ProgressTrack
import com.dscorp.ispadmin.presentation.designsystem.components.SectionHeader
import com.dscorp.ispadmin.presentation.designsystem.components.StatusBadge
import com.dscorp.ispadmin.presentation.theme.IspSpacing
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.theme.IspType

private const val PERCENT = 100f

/** "Cobranza operativa": invoices about to fall due, deferred cuts and this month's cuts vs restores. */
@Composable
fun CollectionsSection(summary: CollectionSummary, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = IspSpacing.lg).testTag("financial_collections"), verticalArrangement = Arrangement.spacedBy(IspSpacing.sm)) {
        SectionHeader(stringResource(R.string.collections_title), caption = stringResource(R.string.collections_caption))
        IspCard {
            DueSoonRow(summary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DeferralsRow(summary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            CutsRow(summary)
        }
    }
}

@Composable
private fun DueSoonRow(summary: CollectionSummary) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Label(stringResource(R.string.collections_due_soon, summary.dueSoonDays))
            Text(formatSoles(summary.dueSoonAmount.toDouble()), style = IspType.kpi)
            Label(stringResource(R.string.collections_invoices, formatCount(summary.dueSoonInvoices)))
        }
        if (summary.dueSoonInvoices > 0) StatusBadge(stringResource(R.string.collections_due_badge), BadgeTone.Warning)
    }
}

@Composable
private fun DeferralsRow(summary: CollectionSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(IspSpacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Label(stringResource(R.string.collections_active_deferrals))
                Text(formatCount(summary.activeDeferrals), style = IspType.kpi)
            }
            if (summary.deferralsExpiringToday > 0) {
                StatusBadge(stringResource(R.string.collections_expiring_today, formatCount(summary.deferralsExpiringToday)), BadgeTone.Warning)
            }
        }
        summary.deferralsKeptPercent?.let { kept ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(IspSpacing.sm)) {
                ProgressTrack(kept.toFloat() / PERCENT, IspTheme.colors.chartCollected, Modifier.weight(1f))
                Label(stringResource(R.string.collections_kept, formatPercent(kept), summary.deferralWindowDays))
            }
        }
    }
}

@Composable
private fun CutsRow(summary: CollectionSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(IspSpacing.sm)) {
        Label(stringResource(R.string.collections_cuts_vs_restored))
        HorizontalBarList(listOf(
            BarItem(stringResource(R.string.collections_cut), formatCount(summary.cutsForDebt), summary.cutsForDebt.toDouble(), IspTheme.colors.chartCategorical[1]),
            BarItem(stringResource(R.string.collections_restored), formatCount(summary.restoredAfterPayment), summary.restoredAfterPayment.toDouble(), IspTheme.colors.chartCollected),
        ))
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}


