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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dscorp.ispadmin.domain.model.Outlay
import com.dscorp.ispadmin.presentation.ui.components.MultiplePhotoAndGalleryPicker
import com.dscorp.ispadmin.presentation.ui.components.rememberPhotoTaker
import com.dscorp.ispadmin.presentation.ui.features.dialog.MyConfirmDialog
import com.dscorp.ispadmin.presentation.ui.features.migration.Loader
import kotlinx.coroutines.flow.StateFlow
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
            ShowErrorDialog(uiState.error ?: "", onDismiss = { viewModel.clearError() })
        }

        uiState.isSaved -> ShowSuccessDialog { viewModel.clearError() }
    }

    RegisterOutLayForm(
        onRegisterClick = { amount, description, code, category, receiptUrl, costCenter ->
            val outLay = Outlay(
                amount = amount.toDouble(),
                description = description,
                document_code = code,
                category = category,
                receipt_url = receiptUrl,
                cost_center = costCenter
            )

            viewModel.registerOutLay(outLay)
        },
        onPhotoTaken = { viewModel.handleIntent(OutlayIntent.TakeImage(it)) },
        onSave = {},
        onPhotoRemove = { viewModel.handleIntent(OutlayIntent.RemoveImage(it)) },
        onImageClick = {
            onImageClick(
                uiState.photoList.map { uri -> uri.toString() },
                uiState.photoList.indexOf(it)
            )
        },
        photoUriList = uiState.photoList
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterOutLayForm(
    modifier: Modifier = Modifier,
    onRegisterClick: (amount: String, description: String, code: String, category: String, receiptUrl: String, costCenter: String) -> Unit,
    onPhotoTaken: (Uri) -> Unit,
    onSave: () -> Unit,
    onPhotoRemove: (Int) -> Unit,
    onImageClick: (Uri) -> Unit,
    photoUriList: List<Uri>

) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var receiptUrl by remember { mutableStateOf("") }
    var costCenter by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var costCenterExpanded by remember { mutableStateOf(false) }

    // Opciones predefinidas para categorías y centros de costo
    val categories =
        listOf("Suministros", "Mantenimiento", "Equipos", "Servicios", "Transporte", "Otros")
    val costCenters =
        listOf("Administración", "Técnico", "Ventas", "Marketing", "Otros")

    val (requestCameraPermission, photoUri) = rememberPhotoTaker(
        onPhotoTaken = { uri -> onPhotoTaken(uri) }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        photoUri.value = uri
        uri?.let { onPhotoTaken(it) }
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
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = amount,
            trailingIcon = {
                if (amount.isNotEmpty()) {
                    IconButton(onClick = { amount = "" }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Limpiar"
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Monto") },
            onValueChange = {
                if (it.contains(",") || it.contains(" ") || it.contains("-") || it.contains("\n") || it.length > 10) return@OutlinedTextField
                else amount = it
            }
        )

        // Campo Descripción
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = description,
            trailingIcon = {
                if (description.isNotEmpty()) {
                    IconButton(onClick = { description = "" }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Limpiar"
                        )
                    }
                }
            },
            label = { Text("Descripción") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            onValueChange = {
                if (it.length > 200) return@OutlinedTextField
                description = it
            }
        )

        // Campo Código de Documento
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = code,
            trailingIcon = {
                if (code.isNotEmpty()) {
                    IconButton(onClick = { code = "" }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Limpiar"
                        )
                    }
                }
            },
            label = { Text("Código de Documento") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            onValueChange = {
                if (it.length > 50) return@OutlinedTextField
                code = it
            }
        )

        // Dropdown para Categoría
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                value = category,
                label = { Text("Categoría") },
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                },
                onValueChange = {}
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categories.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            category = option
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        // Dropdown para Centro de Costo
        ExposedDropdownMenuBox(
            expanded = costCenterExpanded,
            onExpandedChange = { costCenterExpanded = !costCenterExpanded }
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                value = costCenter,
                label = { Text("Centro de Costo") },
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = costCenterExpanded)
                },
                onValueChange = {}
            )
            ExposedDropdownMenu(
                expanded = costCenterExpanded,
                onDismissRequest = { costCenterExpanded = false }
            ) {
                costCenters.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            costCenter = option
                            costCenterExpanded = false
                        }
                    )
                }
            }
        }

        MultiplePhotoAndGalleryPicker(
            photoUriList = photoUriList,
            onTakePhoto = requestCameraPermission,
            onSelectGalleryImage = { galleryLauncher.launch("image/*") },
            onImageRemoveClick = onPhotoRemove,
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
                    amount = ""
                    description = ""
                    code = ""
                    category = ""
                    receiptUrl = ""
                    costCenter = ""
                }
            ) {
                Text("Limpiar")
            }

            Button(
                modifier = Modifier.weight(1f),
                enabled = amount.isNotEmpty() && description.isNotEmpty() && category.isNotEmpty(),
                onClick = {
                    onRegisterClick(amount, description, code, category, receiptUrl, costCenter)
                }
            ) {
                Text("Registrar")
            }
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterOutLayScreenPreview() {
    RegisterOutLayForm(
        modifier = Modifier,
        onRegisterClick = { amount, description, code, category, receiptUrl, costCenter -> },
        onPhotoTaken = {},
        onSave = {},
        onPhotoRemove = {},
        onImageClick = {},
        photoUriList = mutableListOf()
    )
}




