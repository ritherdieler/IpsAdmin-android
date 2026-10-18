package com.dscorp.ispadmin.presentation.ui.features.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.navigation.MainDestination
import com.dscorp.ispadmin.navigation.ShellChrome
import com.dscorp.ispadmin.presentation.theme.IspTheme

object MainShellTestTags {
    const val BOTTOM_BAR = "main_bottom_bar"
    const val TITLE = "main_top_bar_title"
    fun item(destination: MainDestination) = "main_nav_${destination.name.lowercase()}"
}

/** App frame: title bar (back arrow on secondary screens) and the role's bottom bar on root screens. */
@Composable
fun MainShell(
    chrome: ShellChrome,
    onDestinationSelected: (MainDestination) -> Unit,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { if (chrome.showTopBar) ShellTopBar(chrome, onBack) },
        bottomBar = {
            if (chrome.bottomBar.isNotEmpty()) ShellBottomBar(chrome.bottomBar, chrome.selected, onDestinationSelected)
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellTopBar(chrome: ShellChrome, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = chrome.title,
                fontSize = if (chrome.showBack) 20.sp else 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(MainShellTestTags.TITLE),
            )
        },
        navigationIcon = {
            if (chrome.showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
private fun ShellBottomBar(
    destinations: List<MainDestination>,
    selected: MainDestination?,
    onDestinationSelected: (MainDestination) -> Unit,
) {
    val colors = IspTheme.colors
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag(MainShellTestTags.BOTTOM_BAR),
    ) {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(destination.icon(), contentDescription = null) },
                label = {
                    Text(
                        text = destination.label,
                        fontSize = 12.sp,
                        letterSpacing = 0.sp,
                        fontWeight = if (destination == selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = colors.selectedContainer,
                    selectedIconColor = colors.onSelectedContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.testTag(MainShellTestTags.item(destination)),
            )
        }
    }
}

private fun MainDestination.icon(): ImageVector = when (this) {
    MainDestination.HOME -> Icons.Outlined.Home
    MainDestination.TODAY -> Icons.Outlined.Today
    MainDestination.SUBSCRIPTIONS -> Icons.Outlined.Group
    MainDestination.TICKETS -> Icons.Outlined.ConfirmationNumber
    MainDestination.CASH -> Icons.Outlined.CreditCard
    MainDestination.INSTALLATIONS -> Icons.Outlined.Handyman
    MainDestination.MORE -> Icons.Outlined.GridView
}
