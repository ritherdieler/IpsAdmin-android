package com.dscorp.ispadmin.presentation.ui.features.subscription.edit.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.Loader
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyButton
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyCustomDialog
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyOutLinedDropDown
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyOutlinedTextField
import com.dscorp.ispadmin.presentation.ui.features.subscription.edit.EditSubscriptionUIState
import com.dscorp.ispadmin.domain.model.PlanResponse

@Composable
fun EditPlanSubscriptionScreen(
    state: EditSubscriptionUIState,
    onPlanSelected: (PlanResponse?) -> Unit,
    onEditClick: () -> Unit,
    onSuccessDialogDismiss: () -> Unit,
    onErrorDismiss: () -> Unit
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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = stringResource(id = R.string.edit_plan),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                
                Text(
                    text = "En este formulario solo puede cambiar el plan del suscriptor",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                
                Spacer(modifier = Modifier.height(16.dp))

                // Display subscriber information (read-only)
                state.subscriptionData?.let { subscription ->
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
                                text = stringResource(id = R.string.subscription_details),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            MyOutlinedTextField(
                                value = subscription.firstName ?: "",
                                onValueChange = { },
                                label = stringResource(id = R.string.firstName),
                                modifier = Modifier.fillMaxWidth(),
                                hasError = false,
                                singleLine = true,
                                readOnly = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            MyOutlinedTextField(
                                value = subscription.lastName ?: "",
                                onValueChange = { },
                                label = stringResource(id = R.string.lastName),
                                modifier = Modifier.fillMaxWidth(),
                                hasError = false,
                                singleLine = true,
                                readOnly = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            MyOutlinedTextField(
                                value = subscription.dni ?: "",
                                onValueChange = { },
                                label = stringResource(id = R.string.dni),
                                modifier = Modifier.fillMaxWidth(),
                                hasError = false,
                                singleLine = true,
                                readOnly = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Plan selection field - Editable section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.new_plan),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            MyOutLinedDropDown(
                                items = state.plans,
                                selected = state.form.selectedPlan,
                                onItemSelected = { plan ->
                                    onPlanSelected(plan)
                                },
                                label = stringResource(id = R.string.plan),
                                hasError = !state.form.isValid && state.form.touched,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            if (!state.form.isValid && state.form.touched) {
                                Text(
                                    text = "Por favor seleccione un plan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    MyButton(
                        onClick = onEditClick,
                        text = stringResource(id = R.string.edit),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
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
                            Text(
                                text = stringResource(id = R.string.success),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(id = R.string.edit_plan_success),
                                style = MaterialTheme.typography.bodyMedium
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
                            Text(
                                text = stringResource(id = R.string.error),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium
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
        }
    }
} 
