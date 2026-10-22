package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResume
import com.dscorp.ispadmin.observability.ObservabilityComposeText
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.designsystem.components.ErrorState
import com.dscorp.ispadmin.presentation.designsystem.components.StaleBanner
import com.dscorp.ispadmin.presentation.theme.IspType
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.HubBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.badge

/** Proposal A search (a-sus-01/02): one box, quick accesses and recents at rest, compact rows once searching. */
@Composable
fun SubscriptionSearchContent(
    state: SubscriptionFinderUiState,
    onQueryChange: (String) -> Unit,
    onFilterSelected: (ResultFilter) -> Unit,
    onQuickAccess: (ServiceStatus) -> Unit,
    onOpenSubscription: (SubscriptionResume) -> Unit,
    onOpenRecent: (Int) -> Unit,
    onClearRecents: () -> Unit,
    onOpenFilters: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit = {},
    rowMenu: @Composable (SubscriptionResume, expanded: Boolean, onDismiss: () -> Unit) -> Unit = { _, _, _ -> },
    rowExpansion: @Composable (SubscriptionResume) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SearchHeader(onOpenFilters)
        SearchBox(state = state, onQueryChange = onQueryChange)
        if (state.showStart) {
            StartContent(state, onQuickAccess, onOpenRecent, onClearRecents)
        } else {
            ResultsContent(state, onFilterSelected, onOpenSubscription, onLoadMore, onRetry, rowMenu, rowExpansion)
        }
    }
}

@Composable
private fun SearchHeader(onOpenFilters: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Suscripciones",
            style = IspType.screenTitle,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onOpenFilters,
            modifier = Modifier.size(48.dp).testTag(SubscriptionFinderTestTags.FILTERS),
        ) {
            Icon(Icons.Rounded.FilterList, contentDescription = "Filtros")
        }
    }
}

@Composable
private fun SearchBox(state: SubscriptionFinderUiState, onQueryChange: (String) -> Unit) {
    val change: (String) -> Unit = { text ->
        ObservabilityComposeText.report(tag = SubscriptionFinderTestTags.SMART_QUERY, label = "Buscar suscripción", value = text)
        onQueryChange(text)
    }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OutlinedTextField(
            value = state.smartQuery,
            onValueChange = change,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(SubscriptionFinderTestTags.SMART_QUERY),
            interactionSource = interaction,
            placeholder = { Text("Buscar nombre, DNI, IP o código", fontSize = 16.sp) },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = {
                when {
                    state.isSearching -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    !state.showStart || state.smartQuery.isNotEmpty() -> IconButton(
                        onClick = { change("") },
                        modifier = Modifier.testTag(SubscriptionFinderTestTags.CLEAR_QUERY),
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Borrar búsqueda",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
        Text(
            text = "Reconoce nombre, DNI, IP o código",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun StartContent(
    state: SubscriptionFinderUiState,
    onQuickAccess: (ServiceStatus) -> Unit,
    onOpenRecent: (Int) -> Unit,
    onClearRecents: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        if (state.quickAccess.isNotEmpty()) {
            item { SectionTitle("Accesos rápidos", Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp)) }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.quickAccess.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEach { access ->
                                QuickAccessTile(access, Modifier.weight(1f)) { onQuickAccess(access.status) }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(40.dp).padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle("Recientes", Modifier.weight(1f))
                if (state.recents.isNotEmpty()) {
                    TextButton(
                        onClick = onClearRecents,
                        modifier = Modifier.testTag(SubscriptionFinderTestTags.CLEAR_RECENTS),
                    ) {
                        Text("Borrar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = IspTheme.colors.accentInk)
                    }
                }
            }
        }
        if (state.recents.isEmpty()) {
            item {
                Text(
                    text = "Las suscripciones que abras aparecerán aquí.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        } else {
            item { Spacer(Modifier.height(8.dp)) }
        }
        cardItems(state.recents, key = { "recent_${it.id}" }) { recent, isLast ->
            ListRow(
                initials = initialsOf(recent.name),
                muted = recent.status == ServiceStatus.CANCELLED,
                title = AnnotatedString(recent.name),
                subtitle = recent.subtitle,
                showDivider = !isLast,
                modifier = Modifier.testTag(SubscriptionFinderTestTags.recent(recent.id)),
                onClick = { onOpenRecent(recent.id) },
                trailing = { HubBadge(recent.status.badge()) },
            )
        }
    }
}

@Composable
private fun QuickAccessTile(access: QuickAccess, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp).testTag(SubscriptionFinderTestTags.quickAccess(access.status)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = access.count.toString(), fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = access.label,
                    style = IspType.rowLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ResultsContent(
    state: SubscriptionFinderUiState,
    onFilterSelected: (ResultFilter) -> Unit,
    onOpenSubscription: (SubscriptionResume) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    rowMenu: @Composable (SubscriptionResume, Boolean, () -> Unit) -> Unit,
    rowExpansion: @Composable (SubscriptionResume) -> Unit,
) {
    val results = state.results
    // FilterChip draws 32 dp and reserves a 48 dp touch target, so the row padding drops by those 8 dp per side.
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ResultFilter.entries) { filter ->
            val selected = state.resultFilter == filter
            FilterChip(
                selected = selected,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = IspTheme.colors.selectedContainer,
                    selectedLabelColor = IspTheme.colors.onSelectedContainer,
                    selectedLeadingIconColor = IspTheme.colors.onSelectedContainer,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderWidth = 0.dp,
                ),
                modifier = Modifier.testTag(SubscriptionFinderTestTags.resultFilter(filter)),
            )
        }
    }
    val loadedAny = state.subscriptions.values.any { it.isNotEmpty() }
    // A failed refresh keeps what was found before, with a way to try again.
    if (state.searchError != null && loadedAny && !state.isSearching) {
        StaleBanner("No se pudo actualizar", onRetry, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
    }
    when {
        state.searchError != null && !loadedAny && !state.isSearching -> ErrorState(
            title = "No se pudo buscar",
            message = "Revisa tu conexión y vuelve a intentar.",
            onRetry = onRetry,
        )
        state.isSearching && !loadedAny -> Column(Modifier.padding(horizontal = 16.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column { repeat(3) { SkeletonRow() } }
            }
        }
        results.isEmpty() && state.resultFilter != ResultFilter.ALL && loadedAny -> EmptyMessage(
            title = "Ningún resultado con «${state.resultFilter.label}»",
            hint = null,
            action = "Ver todos" to { onFilterSelected(ResultFilter.ALL) },
        )
        results.isEmpty() && state.searchPerformed -> EmptyMessage(
            title = state.smartQuery.trim().takeIf { it.isNotEmpty() }?.let { "Sin resultados para «$it»" } ?: "Sin resultados",
            hint = "Prueba con el DNI, la IP o el código de la suscripción.",
            action = null,
        )
        else -> {
            Text(
                text = buildAnnotatedString {
                    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface))
                    append(resultCountLabel(results.size, state.canLoadMore))
                    pop()
                    listingContext(state.lastUsedFilter)?.let { append(" · $it") }
                },
                style = IspType.rowLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    .testTag(SubscriptionFinderTestTags.RESULT_COUNT),
            )
            ResultList(state, results, onOpenSubscription, onLoadMore, rowMenu, rowExpansion)
        }
    }
}

@Composable
private fun ResultList(
    state: SubscriptionFinderUiState,
    results: List<SubscriptionResume>,
    onOpenSubscription: (SubscriptionResume) -> Unit,
    onLoadMore: () -> Unit,
    rowMenu: @Composable (SubscriptionResume, Boolean, () -> Unit) -> Unit,
    rowExpansion: @Composable (SubscriptionResume) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, results.size, state.canLoadMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last ->
                if (state.canLoadMore && last >= results.size - LOAD_MORE_THRESHOLD) onLoadMore()
            }
    }
    val highlight = state.smartQuery.takeIf { state.lastUsedFilter is SubscriptionFilter.BY_NAME }.orEmpty()
    val highlightColor = IspTheme.colors.warningContainer
    var menuFor by rememberSaveable { mutableStateOf<Int?>(null) }
    val loadingMore = state.isSearching
    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
        cardItems(results, key = { it.id }, closedBottom = !loadingMore) { subscription, isLast ->
            val row = SubscriptionRowModel.from(subscription)
            Box {
                Column {
                    ListRow(
                        initials = row.initials,
                        muted = subscription.serviceStatus == ServiceStatus.CANCELLED,
                        title = highlighted(row.name, highlight, highlightColor),
                        subtitle = row.subtitle,
                        showDivider = !isLast || loadingMore,
                        modifier = Modifier.testTag(SubscriptionFinderTestTags.resultItem(subscription.id)),
                        onClick = { onOpenSubscription(subscription) },
                        onLongClick = { menuFor = subscription.id },
                        trailing = {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                HubBadge(row.status)
                                Text(
                                    text = row.debt,
                                    fontSize = if (row.owes) 14.sp else 13.sp,
                                    fontWeight = if (row.owes) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (row.owes) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                    rowExpansion(subscription)
                }
                rowMenu(subscription, menuFor == subscription.id) { menuFor = null }
            }
        }
        if (loadingMore) {
            item(key = "load_more") {
                Box(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                ) { SkeletonRow() }
            }
        }
    }
}

/** Rows that read as one white card with 16dp corners, like the design lists. */
private fun <T> LazyListScope.cardItems(
    items: List<T>,
    key: (T) -> Any,
    closedBottom: Boolean = true,
    content: @Composable (T, isLast: Boolean) -> Unit,
) {
    itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
        val isFirst = index == 0
        val isLast = index == items.lastIndex
        val shape: Shape = RoundedCornerShape(
            topStart = if (isFirst) 16.dp else 0.dp,
            topEnd = if (isFirst) 16.dp else 0.dp,
            bottomStart = if (isLast && closedBottom) 16.dp else 0.dp,
            bottomEnd = if (isLast && closedBottom) 16.dp else 0.dp,
        )
        Box(Modifier.padding(horizontal = 16.dp).background(MaterialTheme.colorScheme.surface, shape)) {
            content(item, isLast)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListRow(
    initials: String,
    muted: Boolean,
    title: AnnotatedString,
    subtitle: String,
    showDivider: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Más acciones")
    ) {
        Row(
            modifier = Modifier.heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (muted) MaterialTheme.colorScheme.surfaceContainer else IspTheme.colors.selectedContainer,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else IspTheme.colors.onSelectedContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            trailing()
        }
        if (showDivider) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
    }
}

@Composable
private fun SkeletonRow() {
    val tone = IspTheme.colors.track
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).background(tone, CircleShape))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(168.dp).height(12.dp).background(tone, RoundedCornerShape(8.dp)))
            Box(Modifier.width(112.dp).height(10.dp).background(tone, RoundedCornerShape(8.dp)))
        }
        Box(Modifier.width(56.dp).height(20.dp).background(tone, RoundedCornerShape(8.dp)))
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = IspType.sectionTitle, modifier = modifier)
}

@Composable
private fun EmptyMessage(title: String, hint: String?, action: Pair<String, () -> Unit>?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        hint?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        action?.let { (label, onClick) -> TextButton(onClick = onClick) { Text(label) } }
    }
}

private fun listingContext(filter: SubscriptionFilter?): String? = when (filter) {
    is SubscriptionFilter.BY_STATUS -> filter.status.badge().text
    is SubscriptionFilter.BY_DATE -> "Alta del ${filter.startDate} al ${filter.endDate}"
    else -> null
}

private fun initialsOf(name: String): String =
    name.split(' ').filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }

/** Typed text gets the design's yellow marker behind it. */
private fun highlighted(name: String, query: String, marker: Color): AnnotatedString =
    buildAnnotatedString {
        append(name)
        highlightRanges(name, query).forEach { range ->
            addStyle(SpanStyle(background = marker), range.first, range.last + 1)
        }
    }

private const val LOAD_MORE_THRESHOLD = 3
