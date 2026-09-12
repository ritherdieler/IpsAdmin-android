package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.accessmigration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.presentation.ui.components.CleanDetailField
import com.dscorp.ispadmin.presentation.ui.components.MyButton
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.SubscriptionDetailTestTags

@Composable
fun AccessMigrationContent(
    uiState: AccessMigrationUiState,
    onIntent: (AccessMigrationIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val accessValue = uiState.accessValue
        if (accessValue != null) {
            CleanDetailField(
                icon = if (uiState.accessLabel == AccessMigrationUiState.PPPOE_LABEL) {
                    Icons.Rounded.Wifi
                } else {
                    Icons.Rounded.Router
                },
                label = uiState.accessLabel,
                value = accessValue,
                modifier = Modifier.testTag(SubscriptionDetailTestTags.NETWORK_ACCESS_VALUE),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        uiState.stageMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(SubscriptionDetailTestTags.MIGRATION_STAGE),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        uiState.quarantineUntil?.takeIf { it.isNotBlank() }?.let { until ->
            Text(
                text = "Cuarentena hasta $until",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (uiState.isMigrating) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SubscriptionDetailTestTags.MIGRATION_PROGRESS),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(SubscriptionDetailTestTags.MIGRATION_ERROR),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (uiState.canMigrate) {
            MyButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag(SubscriptionDetailTestTags.MIGRATE_PPPOE),
                text = "Migrar a PPPoE",
                enabled = !uiState.isMigrating,
                isLoading = uiState.isMigrating,
                onClick = { onIntent(AccessMigrationIntent.StartMigration) },
            )
        }
    }
}
