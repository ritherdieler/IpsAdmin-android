package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dscorp.ispadmin.domain.model.CustomerData
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBox
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResume

val filters = listOf(
    SubscriptionFilter.BY_NAME(),
    SubscriptionFilter.BY_DOCUMENT(),
    SubscriptionFilter.BY_DATE()
)

@Composable
fun SubscriptionFinder(
    subscriptions: Map<ServiceStatus, List<SubscriptionResume>>,
    onMenuItemSelected: (menuItem: SubscriptionMenu, subscription: SubscriptionResume) -> Unit = { _, _ -> },
    onSubscriptionExpanded: (SubscriptionResume, Boolean) -> Unit = { _, _ -> },
    expandedSubscriptionId: Int? = null,
    customerFormData: CustomerFormData? = null,
    placesState: PlacesState = PlacesState(),
    saveState: SaveSubscriptionState = SaveSubscriptionState.Success,
    onFieldChange: (String, String) -> Unit = { _, _ -> },
    onPlaceSelected: (Place) -> Unit = {},
    onUpdatePlaceId: (Int, String) -> Unit = { _, _ -> },
    onSaveCustomer: () -> Unit = {}
) {
    var lastScrollOffset by remember { mutableStateOf(1) }
    var scrollingUp by remember { mutableStateOf(0) }

    val scrollState = rememberLazyListState()

    LaunchedEffect(key1 = scrollState) {
        snapshotFlow { scrollState.firstVisibleItemScrollOffset }.collect { offset ->
            scrollingUp = if (offset < lastScrollOffset) 100 else 0
            lastScrollOffset = offset
        }
    }

    SubscriptionFinderContent(
        subscriptions = subscriptions,
        scrollState = scrollState,
        onMenuItemSelected = onMenuItemSelected,
        onSubscriptionExpanded = onSubscriptionExpanded,
        expandedSubscriptionId = expandedSubscriptionId,
        customerFormData = customerFormData,
        placesState = placesState,
        saveState = saveState,
        onFieldChange = onFieldChange,
        onPlaceSelected = onPlaceSelected,
        onUpdatePlaceId = onUpdatePlaceId,
        onSaveCustomer = onSaveCustomer
    )
}

@Composable
fun SubscriptionFinderContent(
    subscriptions: Map<ServiceStatus, List<SubscriptionResume>>,
    scrollState: LazyListState,
    onMenuItemSelected: (menuItem: SubscriptionMenu, subscription: SubscriptionResume) -> Unit,
    onSubscriptionExpanded: (SubscriptionResume, Boolean) -> Unit,
    expandedSubscriptionId: Int? = null,
    customerFormData: CustomerFormData? = null,
    placesState: PlacesState = PlacesState(),
    saveState: SaveSubscriptionState = SaveSubscriptionState.Success,
    onFieldChange: (String, String) -> Unit = { _, _ -> },
    onPlaceSelected: (Place) -> Unit = {},
    onUpdatePlaceId: (Int, String) -> Unit = { _, _ -> },
    onSaveCustomer: () -> Unit = {}
) {
    var filtersVisible by remember { mutableStateOf(true) }
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        SubscriptionList(
            subscriptions = subscriptions,
            scrollState = scrollState,
            onMenuItemSelected = onMenuItemSelected,
            onSubscriptionExpanded = onSubscriptionExpanded,
            expandedSubscriptionId = expandedSubscriptionId,
            customerFormData = customerFormData,
            placesState = placesState,
            saveState = saveState,
            onFieldChange = onFieldChange,
            onPlaceSelected = onPlaceSelected,
            onUpdatePlaceId = onUpdatePlaceId,
            onSaveCustomer = onSaveCustomer
        )
    }

    LaunchedEffect(key1 = scrollState) {
        var lastScrollOffset = scrollState.firstVisibleItemScrollOffset
        snapshotFlow { scrollState.firstVisibleItemScrollOffset }.collect { offset ->
            val newFiltersVisible = offset <= lastScrollOffset
            if (newFiltersVisible != filtersVisible) {
                filtersVisible = newFiltersVisible
            }
            lastScrollOffset = offset
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun SubscriptionFinderPreview() {
    MaterialTheme {
        SubscriptionFinder(subscriptions = emptyMap())
    }
}

fun generateMockSubscriptions(): List<SubscriptionResume> {
    val mockList = mutableListOf<SubscriptionResume>()
    for (i in 1..5) {
        val subscriptionResume = SubscriptionResume(
            id = i,
            planName = "Plan $i",
            customerName = "Cliente $i",
            antiquity = "${i} año(s)",
            qualification = "${i} estrella(s)",
            placeName = "Lugar $i",
            ics = "12345$i",
            lastPaymentDate = "12/12/2${i + 1}",
            pendingInvoicesQuantity = i,
            totalDebt = 100.0 * i,
            ipAddress = "192.168.1.$i",
            customer = CustomerData(
                name = "Nombre $i",
                lastName = "Apellido $i",
                dni = "4827183$i",
                place = "Lugar $i",
                address = "Dirección $i",
                phone = "98765432$i",
                email = "cliente$i@gmail.com",
                subscriptionId = i
            ),
            serviceStatus = ServiceStatus.ACTIVE, installationType = InstallationType.FIBER,
            napBox = NapBox("NapBox $i", "Calle $i", placeName = "placeName", placeId = -1),
            placeId = "",
            location = GeoLocation()

        )
        mockList.add(subscriptionResume)
    }
    return mockList
}
