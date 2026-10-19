package com.dscorp.ispadmin.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch

/**
 * Hands a one-off [value] (snackbar text, downloaded file…) to [handle] and then calls [onConsumed].
 * [handle] runs in the screen scope, so clearing the value in [onConsumed] does not cancel it
 * (doing both inside one LaunchedEffect keyed on the value cancels the effect before it shows anything).
 */
@Composable
fun <T : Any> ConsumeOnce(value: T?, onConsumed: () -> Unit, handle: suspend (T) -> Unit) {
    val scope = rememberCoroutineScope()
    val currentHandle = rememberUpdatedState(handle)
    val currentConsumed = rememberUpdatedState(onConsumed)
    LaunchedEffect(value) {
        val taken = value ?: return@LaunchedEffect
        scope.launch { currentHandle.value(taken) }
        currentConsumed.value()
    }
}
