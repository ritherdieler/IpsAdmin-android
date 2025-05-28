package com.dscorp.ispadmin.presentation.ui.components

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
fun MyIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit={},
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) {
    var isClickable by remember { mutableStateOf(true) }
    
    fun handleClick() {
        if (isClickable) {
            isClickable = false
            onClick()
        }
    }
    
    LaunchedEffect(isClickable) {
        if (!isClickable) {
            delay(500)
            isClickable = true
        }
    }

    IconButton(
        modifier = modifier,
        onClick = { handleClick() },
        enabled = enabled && isClickable
    ) {
        icon()
    }
}
