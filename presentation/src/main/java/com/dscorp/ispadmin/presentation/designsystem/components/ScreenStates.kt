package com.dscorp.ispadmin.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.theme.IspType

/** a-est-03: something failed to load and nothing older can be shown. */
@Composable
fun ErrorState(title: String, message: String, onRetry: () -> Unit, modifier: Modifier = Modifier, lastAttempt: String? = null) {
    val colors = IspTheme.colors
    StateFrame(
        icon = Icons.Rounded.Warning,
        iconBackground = MaterialTheme.colorScheme.errorContainer,
        iconTint = MaterialTheme.colorScheme.error,
        title = title,
        message = message,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 24.dp).heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.hero, contentColor = colors.onHero),
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Reintentar", fontWeight = FontWeight.SemiBold)
        }
        lastAttempt?.let {
            Text("Último intento $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

/** a-est-04: nothing to show yet, with an optional way somewhere useful. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    modifier: Modifier = Modifier,
    onAction: () -> Unit = {},
) {
    val colors = IspTheme.colors
    StateFrame(icon, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant, title, message, modifier) {
        actionLabel?.let {
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = 24.dp).heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.selectedContainer, contentColor = colors.onSelectedContainer),
            ) { Text(it, fontWeight = FontWeight.SemiBold) }
        }
    }
}

/** a-est-05: the last data is still shown, but refreshing it failed. */
@Composable
fun StaleBanner(text: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = IspTheme.colors
    Row(
        modifier.fillMaxWidth().background(colors.warningContainer, RoundedCornerShape(12.dp)).padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = colors.warning, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        TextButton(onClick = onRetry) { Text("Reintentar", fontWeight = FontWeight.SemiBold) }
    }
}

/** a-est-01: placeholder rows while a list loads, announced once to screen readers. */
@Composable
fun ListSkeleton(rows: Int, modifier: Modifier = Modifier, label: String = "Cargando…") {
    val colors = IspTheme.colors
    Column(modifier.fillMaxWidth().background(colors.card, RoundedCornerShape(16.dp)).semantics { contentDescription = label }) {
        repeat(rows) { index ->
            if (index > 0) HorizontalDivider(color = colors.track)
            Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(colors.track, CircleShape))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Bone(Modifier.fillMaxWidth(0.6f).height(12.dp))
                    Bone(Modifier.fillMaxWidth(0.4f).height(10.dp))
                }
                Bone(Modifier.width(56.dp).height(20.dp))
            }
        }
    }
}

/** One grey placeholder shape. */
@Composable
fun Bone(modifier: Modifier) {
    Box(modifier.background(IspTheme.colors.track, RoundedCornerShape(8.dp)))
}

@Composable
private fun StateFrame(
    icon: ImageVector,
    iconBackground: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    message: String,
    modifier: Modifier,
    actions: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).background(iconBackground, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
        }
        Text(title, style = IspType.sectionTitle, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
        Text(message, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        actions()
    }
}
