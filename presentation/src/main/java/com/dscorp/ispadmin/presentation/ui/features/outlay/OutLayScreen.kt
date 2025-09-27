package com.dscorp.ispadmin.presentation.ui.features.outlay

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dscorp.ispadmin.domain.model.Outlay
import com.dscorp.ispadmin.presentation.ui.components.MultiplePhotoAndGalleryPicker
import com.dscorp.ispadmin.presentation.ui.components.MyButton
import com.dscorp.ispadmin.presentation.ui.components.MyIconButton
import com.dscorp.ispadmin.presentation.ui.components.MyOutlinedTextField
import com.dscorp.ispadmin.presentation.ui.components.rememberPhotoTaker
import com.dscorp.ispadmin.presentation.ui.features.dialog.MyConfirmDialog
import com.dscorp.ispadmin.presentation.ui.features.migration.Loader
import org.koin.androidx.compose.koinViewModel


@Composable
fun RegisterOutlayScreen(
    viewModel: OutLayViewModel = koinViewModel(),
    onImageClick: (List<String>, Int) -> Unit

) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> Loader()
        uiState.error != null -> {
            ShowErrorDialog(message = uiState.error ?: "", onDismiss = { viewModel.clearError() })
        }

        uiState.isSaved -> {
            ShowSuccessDialog { viewModel.clearError() }
        }
    }

    RegisterOutLayForm(
        outlay = uiState.outlay,
        photoList = uiState.photoList,
        isLoading = uiState.isLoading,
        onIntent = { viewModel.handleIntent(it) },
        onImageClick = {
            onImageClick(
                uiState.photoList.map { uri -> uri.toString() },
                uiState.photoList.indexOf(it)
            )
        }
    )
}

@Composable
fun ShowErrorDialog(message: String, onDismiss: () -> Unit) {
    MyConfirmDialog(title = "Error", body = {
        Text(text = message)
    }, onDismissRequest = onDismiss)
}

@Composable
fun ShowSuccessDialog(onDismiss: () -> Unit) {
    MyConfirmDialog(title = "Éxito", body = {
        Text(text = "Egreso registrado correctamente")
    }, onDismissRequest = {

    })
}

@Composable
fun RegisterOutLayForm(
    modifier: Modifier = Modifier,
    outlay: Outlay,
    photoList: List<Uri>,
    isLoading: Boolean,
    onIntent: (OutlayIntent) -> Unit,
    onImageClick: (Uri) -> Unit
) {


    val (requestCameraPermission, photoUri) = rememberPhotoTaker(
        onPhotoTaken = { uri -> onIntent(OutlayIntent.TakeImage(uri)) }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        photoUri.value = uri
        uri?.let { onIntent(OutlayIntent.TakeImage(it)) }
    }

    Column(
        modifier = modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Título del formulario
        Text(
            text = "Registro de Egreso",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Campo Monto
        MyOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = outlay.amount ?: "",
            label = "Monto",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            maxLength = 10,
            regex = Regex("^[0-9]*\\.?[0-9]*$"),
            onValueChange = { newValue ->
                if (!newValue.contains(",") && !newValue.contains(" ") && !newValue.contains("-") && !newValue.contains(
                        "\n"
                    )
                ) {
                    onIntent(OutlayIntent.UpdateAmount(newValue))
                }
            },
            trailingIcon = {
                if (outlay.amount?.isNotEmpty() == true) {
                    MyIconButton(
                        onClick = { onIntent(OutlayIntent.UpdateAmount("")) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Limpiar"
                        )
                    }
                }
            }
        )

        // Campo Descripción
        MyOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = outlay.description ?: "",
            label = "Descripción",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            maxLength = 200,
            singleLine = false,
            maxLines = 3,
            onValueChange = { onIntent(OutlayIntent.UpdateDescription(it)) },
            trailingIcon = {
                if (!outlay.description.isNullOrEmpty()) {
                    MyIconButton(
                        onClick = { onIntent(OutlayIntent.UpdateDescription("")) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Limpiar"
                        )
                    }
                }
            }
        )

        // Campo Código de Documento
        MyOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = outlay.document_code ?: "",
            label = "Código de Documento",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            maxLength = 50,
            onValueChange = { onIntent(OutlayIntent.UpdateDocumentCode(it)) },
            trailingIcon = {
                if (!outlay.document_code.isNullOrEmpty()) {
                    MyIconButton(
                        onClick = { onIntent(OutlayIntent.UpdateDocumentCode("")) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Limpiar"
                        )
                    }
                }
            }
        )

        // Campo Categoría
        MyOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = outlay.category ?: "",
            label = "Categoría",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            maxLength = 50,
            onValueChange = { onIntent(OutlayIntent.UpdateCategory(it)) },
            supportingText = {
                if (outlay.category.isNullOrEmpty()) {
                    Text(
                        text = "Ej: Suministros, Mantenimiento, Equipos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingIcon = {
                if (!outlay.category.isNullOrEmpty()) {
                    MyIconButton(
                        onClick = { onIntent(OutlayIntent.UpdateCategory("")) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Limpiar"
                        )
                    }
                }
            }
        )

        // Campo Centro de Costo
        MyOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = outlay.cost_center ?: "",
            label = "Centro de Costo",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            maxLength = 50,
            onValueChange = { onIntent(OutlayIntent.UpdateCostCenter(it)) },
            supportingText = {
                if (outlay.cost_center.isNullOrEmpty()) {
                    Text(
                        text = "Ej: Administración, Técnico, Ventas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingIcon = {
                if (!outlay.cost_center.isNullOrEmpty()) {
                    MyIconButton(
                        onClick = { onIntent(OutlayIntent.UpdateCostCenter("")) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Limpiar"
                        )
                    }
                }
            }
        )

        MultiplePhotoAndGalleryPicker(
            photoUriList = photoList,
            onTakePhoto = requestCameraPermission,
            onSelectGalleryImage = { galleryLauncher.launch("image/*") },
            onImageRemoveClick = { index -> onIntent(OutlayIntent.RemoveImage(index)) },
            onImageClick = onImageClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botones de acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    onIntent(OutlayIntent.UpdateAmount(""))
                    onIntent(OutlayIntent.UpdateDescription(""))
                    onIntent(OutlayIntent.UpdateDocumentCode(""))
                    onIntent(OutlayIntent.UpdateCategory(""))
                    onIntent(OutlayIntent.UpdateCostCenter(""))
                }
            ) {
                Text("Limpiar")
            }

            MyButton(
                modifier = Modifier.weight(1f),
                text = "Registrar",
                enabled = outlay.isValid() && photoList.isNotEmpty() && !isLoading,
                onClick = {
                    onIntent(OutlayIntent.RegisterOutLay)
                }
            )
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterOutLayScreenPreview() {
    RegisterOutLayForm(
        modifier = Modifier,
        outlay = Outlay(),
        photoList = emptyList(),
        isLoading = false,
        onIntent = {},
        onImageClick = {}
    )
}




