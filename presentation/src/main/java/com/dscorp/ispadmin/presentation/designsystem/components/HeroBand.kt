package com.dscorp.ispadmin.presentation.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.presentation.theme.IspShapes
import com.dscorp.ispadmin.presentation.theme.IspSpacing
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.theme.IspType

/** Fixed top of the hero band (tabs, view selector, actions); stays visible while the list scrolls. */
@Composable
fun HeroTop(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = IspTheme.colors
    CompositionLocalProvider(LocalContentColor provides colors.onHero) {
        Column(
            modifier.fillMaxWidth().background(colors.hero)
                .padding(start = IspSpacing.lg, end = IspSpacing.lg, top = IspSpacing.sm, bottom = IspSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(IspSpacing.md),
            content = content,
        )
    }
}

/** Scrolling body of the hero band with the headline figure; the KPI grid below overlaps its bottom edge. */
@Composable
fun HeroBand(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = IspTheme.colors
    CompositionLocalProvider(LocalContentColor provides colors.onHero) {
        Column(
            modifier.fillMaxWidth().background(colors.hero, IspShapes.heroBottom)
                .padding(start = IspSpacing.lg, end = IspSpacing.lg, top = IspSpacing.lg, bottom = HERO_BOTTOM_PADDING),
            verticalArrangement = Arrangement.spacedBy(IspSpacing.md),
            content = content,
        )
    }
}

/** How far the first KPI row rides over the hero band. */
val HERO_OVERLAP = 36.dp
private val HERO_BOTTOM_PADDING = 56.dp

/**
 * First row of the hero band: menu, screen title with a status line below ("Actualizado hace 3 min") and the
 * screen actions on the right. Replaces the shared top app bar on dashboard screens.
 */
@Composable
fun HeroTitleBar(
    title: String,
    subtitle: String?,
    onMenu: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitleTag: String? = null,
    menuTag: String = "hero_menu",
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = IspTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(IspSpacing.xs)) {
        if (onMenu != null) {
            IconButton(onClick = onMenu, modifier = Modifier.size(44.dp).testTag(menuTag)) {
                Icon(Icons.Default.Menu, contentDescription = "Abrir menú", tint = colors.onHero)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = IspType.screenTitle, color = colors.onHero)
            subtitle?.let {
                Text(it, subtitleTag?.let { tag -> Modifier.testTag(tag) } ?: Modifier, style = MaterialTheme.typography.bodySmall,
                    color = colors.onHeroMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        actions()
    }
}


/**
 * Pulls content up by [amount] and shrinks its reported height by the same, so a list item overlaps the
 * item above it (later items draw on top).
 */
fun Modifier.pullUp(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val offset = amount.roundToPx()
    layout(placeable.width, (placeable.height - offset).coerceAtLeast(0)) { placeable.place(0, -offset) }
}

/** Segmented tabs drawn on the hero band. */
@Composable
fun HeroTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = IspTheme.colors
    Row(
        modifier.fillMaxWidth().background(colors.onHero.copy(alpha = 0.08f), IspShapes.pill).padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { index, label ->
            val isSelected = index == selected
            Text(
                label,
                Modifier.weight(1f)
                    .background(if (isSelected) colors.tabSelected else Color.Transparent, IspShapes.pill)
                    .selectable(isSelected, role = Role.Tab, onClick = { onSelect(index) })
                    .heightIn(min = 36.dp).padding(vertical = 10.dp)
                    .testTag("dashboard_tab_$index"),
                color = if (isSelected) colors.onTabSelected else colors.onHero,
                style = IspType.label,
                textAlign = TextAlign.Center,
            )
        }
    }
}

data class HeroChip(val label: String, val selected: Boolean, val tag: String, val onClick: () -> Unit)

@Composable
fun HeroChips(chips: List<HeroChip>, modifier: Modifier = Modifier) {
    val colors = IspTheme.colors
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(IspSpacing.sm)) {
        chips.forEach { chip ->
            Surface(
                selected = chip.selected,
                onClick = chip.onClick,
                modifier = Modifier.heightIn(min = 36.dp).testTag(chip.tag),
                shape = IspShapes.pill,
                color = if (chip.selected) colors.selectedContainer else Color.Transparent,
                contentColor = if (chip.selected) colors.onSelectedContainer else colors.onHero,
                border = if (chip.selected) null else BorderStroke(1.dp, colors.onHeroMuted),
            ) {
                Text(chip.label, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = IspType.label)
            }
        }
    }
}

data class HeroBadge(val text: String, val tone: DeltaTone)

data class HeroFigureModel(
    val label: String,
    val value: String,
    val tag: String = "dashboard_hero_figure",
    val delta: KpiDelta? = null,
    val badge: HeroBadge? = null,
    val supporting: String? = null,
)

/** The one headline figure of the screen; clickable when it opens the records behind it. */
@Composable
fun HeroFigure(model: HeroFigureModel, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val colors = IspTheme.colors
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(modifier.then(clickable).testTag(model.tag), verticalArrangement = Arrangement.spacedBy(IspSpacing.xs)) {
        Overline(model.label, colors.onHeroMuted)
        Text(model.value, Modifier.testTag("${model.tag}_value"), style = IspType.kpiHero, color = colors.onHero)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(IspSpacing.sm), verticalArrangement = Arrangement.spacedBy(IspSpacing.xs)) {
            model.delta?.let { DeltaText(it, Modifier.align(Alignment.CenterVertically), onHero = true, style = IspType.heroDelta) }
            model.badge?.let { HeroBadgeChip(it) }
        }
        model.supporting?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onHeroMuted) }
    }
}

@Composable
private fun HeroBadgeChip(badge: HeroBadge) {
    val colors = IspTheme.colors
    val tint = when (badge.tone) {
        DeltaTone.Positive -> colors.heroPositive
        DeltaTone.Negative -> colors.heroNegative
        DeltaTone.Neutral -> colors.heroWarning
    }
    Text(badge.text, Modifier.background(tint.copy(alpha = 0.16f), IspShapes.badge).padding(horizontal = 8.dp, vertical = 3.dp),
        style = IspType.kpiDelta, color = tint)
}
