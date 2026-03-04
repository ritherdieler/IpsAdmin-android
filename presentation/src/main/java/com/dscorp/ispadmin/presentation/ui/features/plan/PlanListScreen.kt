package com.dscorp.ispadmin.presentation.ui.features.plan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.Plan
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.presentation.ui.components.Loader
import com.dscorp.ispadmin.presentation.ui.components.MyButton
import com.dscorp.ispadmin.presentation.ui.components.MyCustomDialog
import com.dscorp.components.components.formfields.MyOutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanListScreen(
    state: PlanListUIState,
    onPlanSelected: (PlanResponse) -> Unit,
    onUpdateClick: (Plan) -> Unit,
    onSuccessDialogDismiss: () -> Unit,
    onErrorDismiss: () -> Unit,
    onShowEditDialog: (PlanResponse) -> Unit,
    onHideEditDialog: () -> Unit,
    onUpdateEditedName: (String) -> Unit,
    onUpdateEditedPrice: (String) -> Unit,
    onUpdateEditedDownloadSpeed: (String) -> Unit,
    onUpdateEditedUploadSpeed: (String) -> Unit,
    onUpdateSelectedType: (InstallationType?) -> Unit,
    onToggleFilterExpanded: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = state.isFilterExpanded,
                    onExpandedChange = { onToggleFilterExpanded() }
                ) {
                    OutlinedTextField(
                        value = state.selectedType?.toString() ?: stringResource(id = R.string.all),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(id = R.string.filter)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.isFilterExpanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = state.isFilterExpanded,
                        onDismissRequest = { onToggleFilterExpanded() }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.all)) },
                            onClick = {
                                onUpdateSelectedType(null)
                                onToggleFilterExpanded()
                            }
                        )
                        InstallationType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.toString()) },
                                onClick = {
                                    onUpdateSelectedType(type)
                                    onToggleFilterExpanded()
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(state.filteredPlans) { plan ->
                        PlanCard(
                            plan = plan,
                            onEditClick = {
                                onPlanSelected(plan)
                                onShowEditDialog(plan)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Show loading indicator
            if (state.isLoading) {
                Loader()
            }

            // Show success dialog
            if (state.isSuccess) {
                MyCustomDialog(
                    onDismissRequest = onSuccessDialogDismiss,
                    content = {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(id = R.string.success),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(id = R.string.plan_updated_success),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            MyButton(
                                onClick = onSuccessDialogDismiss,
                                text = stringResource(id = R.string.ok),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                )
            }

            // Show error dialog
            state.error?.let { error ->
                MyCustomDialog(
                    onDismissRequest = onErrorDismiss,
                    content = {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(id = R.string.error),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            MyButton(
                                onClick = onErrorDismiss,
                                text = stringResource(id = R.string.ok),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                )
            }

            // Show edit dialog
            if (state.showEditDialog && state.selectedPlan != null) {
                MyCustomDialog(
                    onDismissRequest = onHideEditDialog,
                    content = {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.edit_plan),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            MyOutlinedTextField(
                                value = state.editedName,
                                onValueChange = onUpdateEditedName,
                                label = stringResource(id = R.string.plan_name),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            MyOutlinedTextField(
                                value = state.editedPrice,
                                onValueChange = onUpdateEditedPrice,
                                label = stringResource(id = R.string.price),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            MyOutlinedTextField(
                                value = state.editedDownloadSpeed,
                                onValueChange = onUpdateEditedDownloadSpeed,
                                label = stringResource(id = R.string.download_speed),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            MyOutlinedTextField(
                                value = state.editedUploadSpeed,
                                onValueChange = onUpdateEditedUploadSpeed,
                                label = stringResource(id = R.string.upload_speed),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            MyButton(
                                onClick = {
                                    state.selectedPlan?.let { plan ->
                                        onUpdateClick(
                                            Plan(
                                                id = plan.id,
                                                name = state.editedName,
                                                price = state.editedPrice.toDoubleOrNull() ?: 0.0,
                                                downloadSpeed = state.editedDownloadSpeed,
                                                uploadSpeed = state.editedUploadSpeed
                                            )
                                        )
                                    }
                                },
                                text = stringResource(id = R.string.update),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            MyButton(
                                onClick = onHideEditDialog,
                                text = stringResource(id = R.string.cancel),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PlanCard(
    plan: PlanResponse,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = plan.name ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.price_format, plan.price ?: 0.0),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(id = R.string.speed_format, plan.downloadSpeed ?: "", plan.uploadSpeed ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            MyButton(
                onClick = onEditClick,
                text = stringResource(id = R.string.edit),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
} 