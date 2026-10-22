package com.dscorp.ispadmin.presentation.ui.features.composecomponents

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.dscorp.ispadmin.presentation.theme.IspShapes
import kotlinx.coroutines.launch

/**
 * Design system snackbar host: Material inverse-surface snackbar with the 8 dp radius-sm corners
 * (Material's default is 4 dp, and changing shapes.extraSmall would also reshape text fields and menus).
 */
@Composable
fun IspSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data -> Snackbar(data, shape = IspShapes.badge) }
}

/**
 * Componente reutilizable para mostrar snackbars con mensajes de éxito o error.
 * 
 * @param snackbarHostState Estado del SnackbarHost
 * @param message Mensaje a mostrar en el snackbar
 * @param actionLabel Etiqueta del botón de acción (opcional)
 * @param duration Duración del snackbar
 * @param onDismiss Callback que se ejecuta cuando el snackbar se cierra
 * @param onAction Callback que se ejecuta cuando se presiona el botón de acción
 */
@Composable
fun ReusableSnackbar(
    snackbarHostState: SnackbarHostState,
    message: String,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Long,
    onDismiss: (() -> Unit)? = null,
    onAction: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(message) {
        if (message.isNotEmpty()) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    duration = duration
                )
                
                when (result) {
                    SnackbarResult.Dismissed -> onDismiss?.invoke()
                    SnackbarResult.ActionPerformed -> onAction?.invoke()
                }
            }
        }
    }
}

/**
 * Componente específico para mostrar snackbars de éxito.
 * 
 * @param snackbarHostState Estado del SnackbarHost
 * @param successMessage Mensaje de éxito a mostrar
 * @param onDismiss Callback que se ejecuta cuando el snackbar se cierra
 */
@Composable
fun SuccessSnackbar(
    snackbarHostState: SnackbarHostState,
    successMessage: String,
    onDismiss: (() -> Unit)? = null
) {
    ReusableSnackbar(
        snackbarHostState = snackbarHostState,
        message = successMessage,
        actionLabel = "Cerrar",
        duration = SnackbarDuration.Long,
        onDismiss = onDismiss,
        onAction = onDismiss
    )
}

/**
 * Componente específico para mostrar snackbars de error.
 * 
 * @param snackbarHostState Estado del SnackbarHost
 * @param errorMessage Mensaje de error a mostrar
 * @param onDismiss Callback que se ejecuta cuando el snackbar se cierra
 */
@Composable
fun ErrorSnackbar(
    snackbarHostState: SnackbarHostState,
    errorMessage: String,
    onDismiss: (() -> Unit)? = null
) {
    ReusableSnackbar(
        snackbarHostState = snackbarHostState,
        message = errorMessage,
        actionLabel = "Cerrar",
        duration = SnackbarDuration.Indefinite,
        onDismiss = onDismiss,
        onAction = onDismiss
    )
}
