package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResume
import com.dscorp.ispadmin.observability.ObservabilityComposeText
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.HubBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.badge

/** Proposal A search: one box, quick accesses and recents at rest, compact rows with chips once searching. */
@Composable
fun SubscriptionSearchContent(
    state: SubscriptionFinderUiState,
    onQueryChange: (String) -> Unit,
    onFilterSelected: (ResultFilter) -> Unit,
    onQuickAccess: (ServiceStatus) -> Unit,
    onOpenSubscription: (SubscriptionResume) -> Unit,
    onOpenRecent: (Int) -> Unit,
    onClearRecents: () -> Unit,
    onSearchByDate: () -> Unit,
    onLoadMore: () -> Unit,
    rowMenu: @Composable (SubscriptionResume) -> Unit = {},
    rowExpansion: @Composable (SubscriptionResume) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SearchBox(state = state, onQueryChange = onQueryChange, onSearchByDate = onSearchByDate)
        if (state.showStart) {
            StartContent(state, onQuickAccess, onOpenRecent, onClearRecents)
        } else {
            ResultsContent(state, onFilterSelected, onOpenSubscription, onLoadMore, rowMenu, rowExpansion)
        }
    }
}

@Composable
private fun SearchBox(
    state: SubscriptionFinderUiState,
    onQueryChange: (String) -> Unit,
    onSearchByDate: () -> Unit,
) {
    val kind = SmartQuery.kindLabel(state.smartQuery)
    val change: (String) -> Unit = { text ->
        ObservabilityComposeText.report(tag = SubscriptionFinderTestTags.SMART_QUERY, label = "Buscar suscripción", value = text)
        onQueryChange(text)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        OutlinedTextField(
            value = state.smartQuery,
            onValueChange = change,
            modifier = Modifier.weight(1f).testTag(SubscriptionFinderTestTags.SMART_QUERY),
            placeholder = { Text("Nombre, DNI, IP o código") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                when {
                    state.isSearching -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    !state.showStart -> IconButton(
                        onClick = { change("") },
                        modifier = Modifier.testTag(SubscriptionFinderTestTags.CLEAR_QUERY),
                    ) { Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda") }
                }
            },
            supportingText = { Text(kind?.let { "Buscando por $it" } ?: "Reconoce nombre, DNI, IP o código") },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
        IconButton(
            onClick = onSearchByDate,
            modifier = Modifier.padding(top = 4.dp).testTag(SubscriptionFinderTestTags.SEARCH_BY_DATE),
        ) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = "Buscar por fecha de alta")
        }
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
            item { SectionHeader("Accesos rápidos") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.quickAccess.forEach { access ->
                        QuickAccessCard(access, Modifier.weight(1f)) { onQuickAccess(access.status) }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recientes",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                if (state.recents.isNotEmpty()) {
                    TextButton(
                        onClick = onClearRecents,
                        modifier = Modifier.testTag(SubscriptionFinderTestTags.CLEAR_RECENTS),
                    ) { Text("Borrar") }
                }
            }
        }
        if (state.recents.isEmpty()) {
            item {
                Text(
                    text = "Las suscripciones que abras aparecerán aquí.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        items(state.recents, key = { "recent_${it.id}" }) { recent ->
            ListRow(
                initials = initialsOf(recent.name),
                title = AnnotatedString(recent.name),
                subtitle = recent.subtitle,
                modifier = Modifier.testTag(SubscriptionFinderTestTags.recent(recent.id)),
                onClick = { onOpenRecent(recent.id) },
                trailing = { HubBadge(recent.status.badge()) },
            )
        }
    }
}

@Composable
private fun QuickAccessCard(access: QuickAccess, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.testTag(SubscriptionFinderTestTags.quickAccess(access.status)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = access.count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = access.label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ResultsContent(
    state: SubscriptionFinderUiState,
    onFilterSelected: (ResultFilter) -> Unit,
    onOpenSubscription: (SubscriptionResume) -> Unit,
    onLoadMore: () -> Unit,
    rowMenu: @Composable (SubscriptionResume) -> Unit,
    rowExpansion: @Composable (SubscriptionResume) -> Unit,
) {
    val results = state.results
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ResultFilter.entries) { filter ->
            FilterChip(
                selected = state.resultFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.testTag(SubscriptionFinderTestTags.resultFilter(filter)),
            )
        }
    }
    val context = listingContext(state.lastUsedFilter)
    val loadedAny = state.subscriptions.values.any { it.isNotEmpty() }
    when {
        state.isSearching && !loadedAny -> Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
            CircularProgressIndicator()
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
                text = listOfNotNull(resultCountLabel(results.size, state.canLoadMore), context).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
    rowMenu: @Composable (SubscriptionResume) -> Unit,
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
    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
        items(results, key = { it.id }) { subscription ->
            val row = SubscriptionRowModel.from(subscription)
            Column {
                ListRow(
                    initials = row.initials,
                    title = highlighted(row.name, highlight, MaterialTheme.colorScheme.primary),
                    subtitle = row.subtitle,
                    modifier = Modifier.testTag(SubscriptionFinderTestTags.resultItem(subscription.id)),
                    onClick = { onOpenSubscription(subscription) },
                    trailing = {
                        Column(horizontalAlignment = Alignment.End) {
                            HubBadge(row.status)
                            Text(
                                text = row.debt,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (row.owes) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        rowMenu(subscription)
                    },
                )
                rowExpansion(subscription)
            }
        }
        if (state.isSearching) {
            item(key = "load_more") {
                Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun ListRow(
    initials: String,
    title: AnnotatedString,
    subtitle: String,
    modifier: Modifier,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            trailing()
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 68.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
    )
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

private fun highlighted(name: String, query: String, color: androidx.compose.ui.graphics.Color): AnnotatedString =
    buildAnnotatedString {
        append(name)
        highlightRanges(name, query).forEach { range ->
            addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color), range.first, range.last + 1)
        }
    }

private const val LOAD_MORE_THRESHOLD = 3
