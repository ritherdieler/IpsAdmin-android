package com.dscorp.ispadmin.presentation.ui.features.report

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.dscorp.ispadmin.domain.model.DownloadDocumentResponse
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.components.ConsumeOnce
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.IspSnackbarHost
import org.koin.androidx.compose.koinViewModel
import java.io.File

private const val XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

/** a-mas-07 «Reportes». */
@Composable
fun ReportsContent(state: ReportsUiState, onIntent: (ReportsIntent) -> Unit) {
    val colors = IspTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Se descargan en Excel (XLSX) y se abren con la app que elijas.", fontSize = 14.sp, color = muted)
        Column(Modifier.fillMaxWidth().background(colors.card, RoundedCornerShape(16.dp))) {
            state.items.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(Modifier.padding(start = 64.dp), color = colors.track)
                val generating = item.type in state.generating
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 68.dp).clickable(enabled = !generating) { onIntent(ReportsIntent.Download(item.type)) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(Modifier.size(36.dp).background(colors.successContainer, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.TableChart, contentDescription = null, tint = colors.success, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (generating) "Generando…" else item.subtitle, fontSize = 13.sp, color = muted)
                    }
                    if (generating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Rounded.Download, contentDescription = "Descargar", tint = muted)
                }
            }
        }
    }
}

/** Saves each report into the app cache, offers to open it and reports failures. */
@Composable
fun ReportsRoute() {
    val viewModel: ReportsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    ConsumeOnce(state.ready, onConsumed = { viewModel.onIntent(ReportsIntent.Opened) }) { document ->
        val file = runCatching { context.saveReport(document) }.getOrNull()
        if (file == null) {
            snackbar.showSnackbar("No se pudo guardar ${document.getNameWithExtension()}")
            return@ConsumeOnce
        }
        val result = snackbar.showSnackbar("${document.getNameWithExtension()} descargado", actionLabel = "Abrir", duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed && !context.openReport(file)) {
            snackbar.showSnackbar("Instala una app que abra archivos de Excel para verlo")
        }
    }
    ConsumeOnce(state.error, onConsumed = { viewModel.onIntent(ReportsIntent.ErrorShown) }) { snackbar.showSnackbar(it) }
    Box(Modifier.fillMaxSize()) {
        ReportsContent(state, viewModel::onIntent)
        IspSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

private fun Context.saveReport(document: DownloadDocumentResponse): File {
    val folder = File(cacheDir, "reports").apply { mkdirs() }
    return File(folder, document.getNameWithExtension()).apply { writeBytes(Base64.decode(document.base64, Base64.DEFAULT)) }
}

private fun Context.openReport(file: File): Boolean {
    val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
    val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, XLSX)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        startActivity(view)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
