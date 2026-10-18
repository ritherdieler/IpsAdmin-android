package com.dscorp.ispadmin.presentation.ui.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.dscorp.ispadmin.presentation.designsystem.charts.ChartData
import com.dscorp.ispadmin.presentation.designsystem.charts.ChartSeries
import com.dscorp.ispadmin.presentation.designsystem.charts.IspColumnChart
import com.dscorp.ispadmin.presentation.designsystem.charts.averageCaption
import com.dscorp.ispadmin.presentation.designsystem.components.CardAction
import com.dscorp.ispadmin.presentation.designsystem.components.ChartCard
import com.dscorp.ispadmin.presentation.designsystem.components.ChartCardHeader
import com.dscorp.ispadmin.presentation.designsystem.components.HERO_OVERLAP
import com.dscorp.ispadmin.presentation.designsystem.components.HeroBand
import com.dscorp.ispadmin.presentation.designsystem.components.HeroFigure
import com.dscorp.ispadmin.presentation.designsystem.components.HeroFigureModel
import com.dscorp.ispadmin.presentation.designsystem.components.KpiModel
import com.dscorp.ispadmin.presentation.designsystem.components.KpiTile
import com.dscorp.ispadmin.presentation.designsystem.components.pullUp
import com.dscorp.ispadmin.presentation.theme.IspSpacing
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles

/** Month collected so far: the hero figure of the home (a-ini-01). */
@Composable
fun AdminHomeHero(home: AdminHome) {
    HeroBand {
        HeroFigure(HeroFigureModel(home.monthTitle, home.monthAmount, "home_month", delta = home.monthDelta))
    }
}

/** 2×2 grid riding over the hero band. */
@Composable
fun AdminHomeTiles(home: AdminHome, onAction: (AdminHomeAction) -> Unit) {
    Column(Modifier.pullUp(HERO_OVERLAP + IspSpacing.md).padding(horizontal = IspSpacing.lg), verticalArrangement = Arrangement.spacedBy(IspSpacing.md)) {
        home.tiles.chunked(2).forEach { row ->
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(IspSpacing.md)) {
                row.forEach { tile ->
                    KpiTile(
                        KpiModel(tile.label, tile.value, "home_tile_${tile.kind.name.lowercase()}", supporting = tile.supporting),
                        Modifier.weight(1f).fillMaxHeight(),
                    ) { onAction(AdminHomeAction.Tile(tile.kind)) }
                }
            }
        }
    }
}

@Composable
fun AdminDailyCard(home: AdminHome, onAction: (AdminHomeAction) -> Unit) {
    val subtitle = listOfNotNull(averageCaption(home.dailyValues, ::formatSoles), home.todayCaption).joinToString(" · ")
    ChartCard(
        ChartCardHeader(home.dailyTitle, subtitle, action = CardAction("Ver caja") { onAction(AdminHomeAction.Cash) }),
        Modifier.padding(horizontal = IspSpacing.lg).testTag("home_daily_chart"),
    ) {
        if (home.dailyValues.isEmpty()) {
            Text("Aún no hay cobros este mes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            IspColumnChart(
                ChartData(home.dailyLabels, listOf(ChartSeries("Cobrado", IspTheme.colors.chartPrimary, home.dailyValues))),
                markerFormat = ::formatSoles,
                showAverage = true,
            )
        }
    }
}
