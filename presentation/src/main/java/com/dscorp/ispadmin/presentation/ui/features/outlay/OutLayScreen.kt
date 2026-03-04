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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dscorp.ispadmin.domain.model.Outlay
import com.dscorp.ispadmin.presentation.ui.components.MultiplePhotoAndGalleryPicker
import com.dscorp.ispadmin.presentation.ui.components.Loader
import com.dscorp.ispadmin.presentation.ui.components.MyButton
import com.dscorp.ispadmin.presentation.ui.components.MyIconButton
import com.dscorp.components.components.formfields.MyOutlinedTextField
import com.dscorp.ispadmin.presentation.ui.components.MyOutLinedDropDown
import com.dscorp.ispadmin.presentation.ui.components.rememberPhotoTaker
import com.dscorp.ispadmin.presentation.ui.features.dialog.MyConfirmDialog
import org.koin.androidx.compose.koinViewModel

object OutlayConstants {
    val CATEGORIES = listOf(
        "Suministros",
        "Mantenimiento", 
        "Equipos",
        "Servicios",
        "Transporte",
        "Comunicaciones",
        "Seguros",
        "Capacitación",
        "Otros"
    )
    
    val COST_CENTERS = listOf(
        "Administración",
        "Técnico",
        "Ventas", 
        "Marketing",
        "Recursos Humanos",
        "Finanzas",
        "Operaciones",
        "Soporte Técnico",
        "Desarrollo",
        "Otros"
    )
}

@Composable
private fun ClearableTextField(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    keyboardOptions: KeyboardOptions,
    maxLength: Int,
    regex: Regex? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    contentDescription: String = "Limpiar campo"
) {
    MyOutlinedTextField(
        modifier = modifier,
        value = value,
        label = label,
        keyboardOptions = keyboardOptions,
        maxLength = maxLength,
        regex = regex,
        singleLine = singleLine,
        maxLines = maxLines,
        onValueChange = onValueChange,
        trailingIcon = {
            if (value.isNotEmpty()) {
                MyIconButton(
                    onClick = onClear
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = contentDescription
                    )
                }
            }
        }
    )
}

@Composable
fun RegisterOutlayScreen(
    viewModel: OutLayViewModel = koinViewModel(),
    onImageClick: (List<String>, Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> Loader()
        uiState.error != null -> {
            ShowErrorDialog(
                message = uiState.error ?: "Error desconocido",
                onDismiss = { viewModel.clearError() }
            )
        }
        uiState.isSaved -> {
            ShowSuccessDialog(
                onDismiss = { viewModel.clearSaved() }
            )
        }
    }

    RegisterOutLayForm(
        outlay = uiState.outlay,
        photoList = uiState.photoList,
        isLoading = uiState.isLoading,
        onIntent = viewModel::handleIntent,
        onImageClick = { uri ->
            val photoStrings = uiState.photoList.map { it.toString() }
            val index = uiState.photoList.indexOf(uri)
            onImageClick(photoStrings, index)
        }
    )
}

@Composable
private fun ShowErrorDialog(
    message: String, 
    onDismiss: () -> Unit
) {
    MyConfirmDialog(
        title = "Error",
        body = {
            Text(text = message)
        },
        onDismissRequest = onDismiss
    )
}

@Composable
private fun ShowSuccessDialog(
    onDismiss: () -> Unit
) {
    MyConfirmDialog(
        title = "Éxito",
        body = {
            Text(text = "Egreso registrado correctamente")
        },
        onDismissRequest = onDismiss
    )
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
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val paddingValue = if (isTablet) 32.dp else 24.dp

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
            .padding(paddingValue)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Registro de Egreso",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        ClearableTextField(
            modifier = Modifier.fillMaxWidth(),
            value = outlay.amount ?: "",
            label = "Monto",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            maxLength = 10,
            regex = Regex("^[0-9]*\\.?[0-9]*$"),
            onValueChange = { newValue ->
                if (isValidDecimalInput(newValue)) {
                    onIntent(OutlayIntent.UpdateAmount(newValue))
                }
            },
            onClear = { onIntent(OutlayIntent.UpdateAmount("")) },
            contentDescription = "Limpiar monto"
        )

        ClearableTextField(
            modifier = Modifier.fillMaxWidth(),
            value = outlay.description ?: "",
            label = "Descripción",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            maxLength = 200,
            singleLine = false,
            maxLines = 3,
            onValueChange = { onIntent(OutlayIntent.UpdateDescription(it)) },
            onClear = { onIntent(OutlayIntent.UpdateDescription("")) },
            contentDescription = "Limpiar descripción"
        )

        ClearableTextField(
            modifier = Modifier.fillMaxWidth(),
            value = outlay.document_code ?: "",
            label = "Código de Documento",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            maxLength = 50,
            onValueChange = { onIntent(OutlayIntent.UpdateDocumentCode(it)) },
            onClear = { onIntent(OutlayIntent.UpdateDocumentCode("")) },
            contentDescription = "Limpiar código de documento"
        )

        MyOutLinedDropDown(
            modifier = Modifier.fillMaxWidth(),
            items = OutlayConstants.CATEGORIES,
            selected = outlay.category,
            label = "Categoría",
            onItemSelected = { category ->
                onIntent(OutlayIntent.UpdateCategory(category))
            }
        )

        MyOutLinedDropDown(
            modifier = Modifier.fillMaxWidth(),
            items = OutlayConstants.COST_CENTERS,
            selected = outlay.cost_center,
            label = "Centro de Costo",
            onItemSelected = { costCenter ->
                onIntent(OutlayIntent.UpdateCostCenter(costCenter))
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
                onClick = { onIntent(OutlayIntent.RegisterOutLay) }
            )
        }
    }
}

private fun isValidDecimalInput(input: String): Boolean {
    if (input.isEmpty()) return true
    
    val regex = Regex("^[0-9]*\\.?[0-9]{0,2}$")
    if (!regex.matches(input)) return false
    
    val dotCount = input.count { it == '.' }
    if (dotCount > 1) return false
    
    val invalidChars = listOf(",", " ", "-", "\n", "+", "e", "E")
    return !input.any { char -> invalidChars.contains(char.toString()) }
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