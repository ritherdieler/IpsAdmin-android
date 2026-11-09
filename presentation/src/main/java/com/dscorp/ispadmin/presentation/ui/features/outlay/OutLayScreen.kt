package com.dscorp.ispadmin.presentation.ui.features.outlay

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dscorp.ispadmin.domain.model.OutlaySaveResult
import com.dscorp.ispadmin.domain.model.OutlayType
import com.dscorp.ispadmin.presentation.designsystem.components.BadgeTone
import com.dscorp.ispadmin.presentation.designsystem.components.InlineNote
import com.dscorp.ispadmin.presentation.designsystem.components.ScreenTopBar
import com.dscorp.ispadmin.presentation.designsystem.components.SegmentedChoice
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.theme.IspType
import com.dscorp.ispadmin.presentation.ui.components.rememberPhotoTaker
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.RegisterFieldLabel
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.RegisterTextField
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

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

    val FIXED_CATEGORIES = listOf("Alquiler", "Servicios", "Planilla", "Impuestos", "Otros")

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

/** Nuevo egreso (a-caj-07): full-screen form with its own close bar; the Core answers the cap and the team alert. */
@Composable
fun RegisterOutlayScreen(
    viewModel: OutLayViewModel = koinViewModel(),
    onClose: () -> Unit,
    onSaved: (OutlaySaveResult) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.handleIntent(OutlayIntent.LoadLimit) }
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            uiState.saved?.let(onSaved)
            viewModel.clearSaved()
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            ScreenTopBar(
                title = if (uiState.canChooseType) "Nuevo egreso" else "Nuevo egreso variable",
                icon = Icons.Rounded.Close,
                iconDescription = "Cerrar",
                onIconClick = onClose,
            )
            RegisterOutLayForm(
                state = uiState,
                onIntent = viewModel::handleIntent,
                onImageClick = { uri ->
                    val photoStrings = uiState.photoList.map { it.toString() }
                    onImageClick(photoStrings, uiState.photoList.indexOf(uri))
                },
                modifier = Modifier.weight(1f),
            )
        }
        uiState.error?.let { message ->
            OutlaySnackbar(
                message = AnnotatedString(message),
                onDismiss = viewModel::clearError,
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 108.dp),
                actionLabel = "Reintentar",
                onAction = {
                    viewModel.clearError()
                    viewModel.handleIntent(OutlayIntent.RegisterOutLay)
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterOutLayForm(
    state: OutlayUiState,
    onIntent: (OutlayIntent) -> Unit,
    onImageClick: (Uri) -> Unit,
    modifier: Modifier = Modifier,
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

    val colors = IspTheme.colors
    val outlay = state.outlay
    val fixed = state.type == OutlayType.FIXED
    val editable = !state.isLoading

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (state.canChooseType) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RegisterFieldLabel("Tipo *")
                    SegmentedChoice(
                        options = listOf("Variable", "Fijo"),
                        selected = if (fixed) 1 else 0,
                        onSelect = { onIntent(OutlayIntent.UpdateType(if (it == 1) OutlayType.FIXED else OutlayType.VARIABLE)) },
                        enabled = editable,
                    )
                }
            }
            LimitNote(state)
            AmountField(
                value = outlay.amount ?: "",
                onValueChange = { newValue -> if (isValidDecimalInput(newValue)) onIntent(OutlayIntent.UpdateAmount(newValue)) },
                error = state.amountError,
                enabled = editable,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RegisterFieldLabel("Categoría *")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (if (fixed) OutlayConstants.FIXED_CATEGORIES else OutlayConstants.CATEGORIES).forEach { option ->
                        OutlayChoiceChip(option, option == outlay.category, editable) { onIntent(OutlayIntent.UpdateCategory(option)) }
                    }
                }
            }
            RegisterTextField(
                "Descripción *", outlay.description ?: "", { if (it.length <= 200) onIntent(OutlayIntent.UpdateDescription(it)) },
                singleLine = false, minHeight = 52.dp, enabled = editable,
            )
            if (!fixed) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RegisterFieldLabel("Foto del comprobante")
                    ReceiptPhotoZone(
                        photo = state.photoList.firstOrNull(),
                        enabled = editable,
                        onTakePhoto = requestCameraPermission,
                        onPickFromGallery = { galleryLauncher.launch("image/*") },
                        onOpen = onImageClick,
                        onRemove = { onIntent(OutlayIntent.RemoveImage(0)) },
                    )
                    Text("Opcional. Adjúntala si tienes boleta o factura.", style = IspType.rowLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(colors.card)) {
            HorizontalDivider(color = colors.cardBorder)
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.disabledReason?.let { Text(it, style = IspType.rowLabel, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Button(
                    onClick = { onIntent(OutlayIntent.RegisterOutLay) },
                    enabled = state.canSubmit,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = if (state.isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = if (state.isLoading) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Registrando…", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    } else {
                        Text(if (fixed) "Registrar egreso fijo" else "Registrar egreso", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

/** What the user needs to know before typing the amount: why there is no cap (fixed) or how much is left (variable). */
@Composable
private fun LimitNote(state: OutlayUiState) {
    val summary = state.summary
    when {
        state.type == OutlayType.FIXED ->
            InlineNote("Los egresos fijos no piden comprobante y no cuentan para el límite mensual de variables.", BadgeTone.Info)
        summary != null -> {
            val month = remember { LocalDate.now(ZoneId.of("America/Lima")).month.getDisplayName(TextStyle.FULL, Locale("es", "PE")) }
            InlineNote(
                buildAnnotatedString {
                    append("Quedan ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(formatSoles(summary.available)) }
                    append(" de ${formatSoles(summary.limit)} para egresos variables en $month.")
                },
                if (summary.level == LimitLevel.InRange) BadgeTone.Info else BadgeTone.Warning,
            )
        }
    }
}

private fun isValidDecimalInput(input: String): Boolean {
    if (input.isEmpty()) return true
    return Regex("^[0-9]*\\.?[0-9]{0,2}$").matches(input)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterOutLayScreenPreview() {
    RegisterOutLayForm(
        state = OutlayUiState(),
        onIntent = {},
        onImageClick = {}
    )
}
