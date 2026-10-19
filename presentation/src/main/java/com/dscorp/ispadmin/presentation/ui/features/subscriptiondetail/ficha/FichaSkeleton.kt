package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.designsystem.components.Bone
import com.dscorp.ispadmin.presentation.theme.IspTheme

/** a-est-02: the ficha's shape (hero, debt card, actions, tabs, cards) while the subscription loads. */
@Composable
fun FichaSkeleton(onBack: () -> Unit) {
    val colors = IspTheme.colors
    val heroBone = colors.onHero.copy(alpha = 0.16f)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).semantics { contentDescription = "Cargando suscripción…" }) {
        Column(
            Modifier.fillMaxWidth().background(colors.hero, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .statusBarsPadding().padding(start = 4.dp, end = 16.dp, bottom = 56.dp),
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = colors.onHero) }
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.width(236.dp).height(22.dp).background(heroBone, RoundedCornerShape(8.dp)))
                Box(Modifier.width(168.dp).height(14.dp).background(heroBone, RoundedCornerShape(8.dp)))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.width(138.dp).height(22.dp).background(heroBone, RoundedCornerShape(8.dp)))
                    Box(Modifier.width(98.dp).height(22.dp).background(heroBone, RoundedCornerShape(8.dp)))
                }
            }
        }
        Row(
            Modifier.padding(horizontal = 16.dp).offset(y = (-40).dp).fillMaxWidth().background(colors.card, RoundedCornerShape(16.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Bone(Modifier.width(104.dp).height(12.dp))
                Bone(Modifier.width(124.dp).height(22.dp))
                Bone(Modifier.width(176.dp).height(12.dp))
            }
            Bone(Modifier.width(108.dp).height(48.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            repeat(5) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Bone(Modifier.size(48.dp))
                    Bone(Modifier.width(44.dp).height(10.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, start = 8.dp)) {
            listOf("Resumen", "Pagos", "Red", "Tickets", "Historial").forEachIndexed { index, tab ->
                Text(tab, fontSize = 14.sp, fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (index == 0) colors.hero else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 14.dp))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) {
                Column(Modifier.fillMaxWidth().background(colors.card, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Bone(Modifier.width(84.dp).height(14.dp))
                    repeat(4) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Bone(Modifier.width(64.dp).height(12.dp))
                            Bone(Modifier.width(140.dp).height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

