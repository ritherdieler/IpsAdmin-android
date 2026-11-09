package com.dscorp.ispadmin.presentation.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Header of a screen that draws its own bar, as in the design system: 64 dp high, a 48 dp icon button (close for a
 * full-screen form, back for a destination) and a 20 sp title.
 */
@Composable
fun ScreenTopBar(
    title: String,
    icon: ImageVector,
    iconDescription: String,
    onIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    Row(modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onIconClick, modifier = Modifier.size(48.dp)) {
            Icon(icon, contentDescription = iconDescription, tint = contentColor, modifier = Modifier.size(22.dp))
        }
        Text(
            title,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            color = contentColor,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
