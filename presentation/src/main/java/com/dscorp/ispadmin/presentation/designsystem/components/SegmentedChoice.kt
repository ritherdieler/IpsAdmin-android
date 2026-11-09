package com.dscorp.ispadmin.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.theme.IspShapes
import com.dscorp.ispadmin.presentation.theme.IspTheme

/** Picker for 2–3 mutually exclusive options (design system «Elegir»): pill outline, the chosen segment in lavender with ✓. */
@Composable
fun SegmentedChoice(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = IspTheme.colors
    val outline = MaterialTheme.colorScheme.outline
    Row(
        modifier.fillMaxWidth().height(40.dp).clip(IspShapes.pill).border(1.dp, outline, IspShapes.pill).selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            if (index > 0) Spacer(Modifier.width(1.dp).fillMaxHeight().background(outline))
            Row(
                Modifier.weight(1f).fillMaxHeight()
                    .background(if (isSelected) colors.selectedContainer else colors.card)
                    .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(index) }),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSelected) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.onSelectedContainer, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = when {
                        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                        isSelected -> colors.onSelectedContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}
