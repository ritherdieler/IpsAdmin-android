package com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyButton
import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus

@Composable
fun TicketCard(
    ticket: AssistanceTicketResponse,
    currentUser: User,
    isLoading: Boolean = false,
    onCardClick: () -> Unit = {},
    onTakeTicket: () -> Unit = {},
    onCloseTicket: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabecera del ticket
            TicketHeader(ticket)
            
            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            // Información del cliente
            InfoItem(
                icon = Icons.Filled.Person,
                label = "Cliente",
                value = ticket.name
            )
            
            InfoItem(
                icon = Icons.Filled.Phone,
                label = "Teléfono",
                value = ticket.phone
            )
            
            ticket.place?.let { place ->
                InfoItem(
                    icon = Icons.Filled.LocationOn,
                    label = "Ubicación",
                    value = place
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Categoría y descripción con mejor formato
            InfoItem(
                icon = Icons.Filled.Category,
                label = "Categoría",
                value = ticket.category,
                valueStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            
            InfoItem(
                icon = Icons.Filled.Description,
                label = "Descripción",
                value = ticket.description,
                maxLines = 3
            )
            
            // Mostrar indicador de imagen si el ticket está cerrado
            if (ticket.status == AssistanceTicketStatus.CLOSED && ticket.sheetImageUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Ver imagen de cierre",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .clickable { onCardClick() }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botones de acción según el estado y permisos
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (getTakeTicketVisibility(ticket, currentUser)) {
                    MyButton(
                        onClick = onTakeTicket,
                        text = "Tomar Ticket",
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        isLoading = isLoading
                    )
                }
                
                if (getCloseTicketVisibility(ticket, currentUser)) {
                    MyButton(
                        onClick = onCloseTicket,
                        text = if (ticket.status == AssistanceTicketStatus.PENDING) "Cancelar" else "Cerrar",
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = if (getTakeTicketVisibility(ticket, currentUser)) 8.dp else 0.dp),
                        isLoading = isLoading
                    )
                }
            }
        }
    }
}

@Composable
private fun TicketHeader(ticket: AssistanceTicketResponse) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indicador de estado con color
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(getStatusColor(ticket.status))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getStatusIcon(ticket.status),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Ticket #${ticket.id}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ticket.getStatusAsString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = getStatusColor(ticket.status)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = ticket.getCreatedAtDateAsString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Indicador de prioridad
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(getPriorityColor(ticket.priority))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PriorityHigh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = ticket.priority,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    maxLines: Int = 1
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 3.dp)
                .size(16.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = value,
                style = valueStyle,
                overflow = TextOverflow.Ellipsis,
                maxLines = maxLines
            )
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun getStatusColor(status: AssistanceTicketStatus): Color {
    return when (status) {
        AssistanceTicketStatus.PENDING -> MaterialTheme.colorScheme.tertiary
        AssistanceTicketStatus.ASSIGNED -> MaterialTheme.colorScheme.primary
        AssistanceTicketStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondary
        AssistanceTicketStatus.RESOLVED, 
        AssistanceTicketStatus.CLOSED -> MaterialTheme.colorScheme.primary
        AssistanceTicketStatus.CANCELLED -> MaterialTheme.colorScheme.error
    }
}

@Composable
private fun getStatusIcon(status: AssistanceTicketStatus): ImageVector {
    return when (status) {
        AssistanceTicketStatus.PENDING -> Icons.Outlined.Pending
        AssistanceTicketStatus.ASSIGNED, 
        AssistanceTicketStatus.IN_PROGRESS -> Icons.Outlined.Pending
        AssistanceTicketStatus.RESOLVED,
        AssistanceTicketStatus.CLOSED -> Icons.Outlined.CheckCircle
        AssistanceTicketStatus.CANCELLED -> Icons.Outlined.Error
    }
}

@Composable
private fun getPriorityColor(priority: String): Color {
    return when (priority.lowercase()) {
        "alta" -> MaterialTheme.colorScheme.error
        "media" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun getTakeTicketVisibility(
    ticket: AssistanceTicketResponse,
    user: User
): Boolean {
    if (user.type != User.UserType.TECHNICIAN && user.type != User.UserType.ADMIN) return false

    return when (ticket.status) {
        AssistanceTicketStatus.PENDING -> true
        AssistanceTicketStatus.ASSIGNED, 
        AssistanceTicketStatus.IN_PROGRESS, 
        AssistanceTicketStatus.RESOLVED, 
        AssistanceTicketStatus.CLOSED -> false
        AssistanceTicketStatus.CANCELLED -> false
    }
}

private fun getCloseTicketVisibility(
    ticket: AssistanceTicketResponse,
    user: User
): Boolean {
    return when {
        ticket.status == AssistanceTicketStatus.PENDING && 
            (user.type == User.UserType.SECRETARY || user.type == User.UserType.ADMIN) -> true
        ticket.status == AssistanceTicketStatus.ASSIGNED && 
            (user.type == User.UserType.TECHNICIAN || user.type == User.UserType.ADMIN) -> true
        else -> false
    }
} 