package com.dscorp.ispadmin.presentation.ui.features.outlay

import androidx.compose.ui.unit.sp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.home.monthShort
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.RegisterFieldLabel
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.RegisterTextField
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
        "Otros",
        "Suministros",
        "Transporte",
        "Mantenimiento",
        "Equipos",
        "Servicios",
        "Comunicaciones",
        "Seguros",
        "Capacitación",
    )
    
    val COST_CENTERS = listOf(
        "Administración",
        "Técnico",
        "Ventas",
        "Desarrollo",
        "Marketing",
        "Recursos Humanos",
        "Finanzas",
        "Operaciones",
        "Soporte Técnico",
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
    onImageClick: (Uri) -> Unit,
    today: java.time.LocalDate = java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")),
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

    val colors = IspTheme.colors
    val hasPhoto = photoList.isNotEmpty()
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            RegisterTextField(
                "Monto *", outlay.amount ?: "",
                { newValue -> if (isValidDecimalInput(newValue)) onIntent(OutlayIntent.UpdateAmount(newValue)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            ChipField("Categoría *", OutlayConstants.CATEGORIES, outlay.category) { onIntent(OutlayIntent.UpdateCategory(it)) }
            ChipField("Centro de costo *", OutlayConstants.COST_CENTERS, outlay.cost_center) { onIntent(OutlayIntent.UpdateCostCenter(it)) }
            RegisterTextField(
                "Descripción *", outlay.description ?: "", { if (it.length <= 200) onIntent(OutlayIntent.UpdateDescription(it)) },
                singleLine = false, minHeight = 88.dp,
            )
            RegisterTextField(
                "N.º de documento", outlay.document_code ?: "", { if (it.length <= 50) onIntent(OutlayIntent.UpdateDocumentCode(it)) },
                helper = "Opcional",
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RegisterFieldLabel("Fecha")
                Text("Hoy, ${today.dayOfMonth} ${monthShort(today)} ${today.year}", fontSize = 16.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RegisterFieldLabel("Foto del comprobante *")
                MultiplePhotoAndGalleryPicker(
                    photoUriList = photoList,
                    onTakePhoto = requestCameraPermission,
                    onSelectGalleryImage = { galleryLauncher.launch("image/*") },
                    onImageRemoveClick = { index -> onIntent(OutlayIntent.RemoveImage(index)) },
                    onImageClick = onImageClick
                )
                if (!hasPhoto) Text("Falta la foto del comprobante", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
        }
        Column(Modifier.fillMaxWidth().background(colors.card)) {
            HorizontalDivider(color = colors.cardBorder)
            Button(
                onClick = { onIntent(OutlayIntent.RegisterOutLay) },
                enabled = outlay.isValid() && hasPhoto && !isLoading,
                modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.hero, contentColor = colors.onHero),
            ) { Text("Registrar egreso", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipField(label: String, options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RegisterFieldLabel(label)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option -> ChoiceChip(option, option == selected) { onSelect(option) } }
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