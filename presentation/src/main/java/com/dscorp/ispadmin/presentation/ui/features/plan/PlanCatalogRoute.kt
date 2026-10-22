package com.dscorp.ispadmin.presentation.ui.features.plan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dscorp.ispadmin.presentation.ui.components.ConsumeOnce
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.IspSnackbarHost
import org.koin.androidx.compose.koinViewModel

/** «Planes» with the editor as a bottom sheet over the list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanCatalogRoute() {
    val viewModel: PlanCatalogViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.onIntent(PlanCatalogIntent.Load) }
    ConsumeOnce(state.message, onConsumed = { viewModel.onIntent(PlanCatalogIntent.MessageShown) }) { snackbar.showSnackbar(it) }
    Box(Modifier.fillMaxSize()) {
        PlanCatalogContent(state, viewModel::onIntent)
        IspSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    state.editor?.let { editor ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.onIntent(PlanCatalogIntent.CloseEditor) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PlanEditorContent(
                    editor = editor,
                    saving = state.saving,
                    error = state.editorError,
                    onChange = { change -> viewModel.onIntent(PlanCatalogIntent.EditorChanged(change)) },
                    onSave = { viewModel.onIntent(PlanCatalogIntent.Save) },
                    onRetire = { viewModel.onIntent(PlanCatalogIntent.Retire) },
                )
            }
        }
    }
}
