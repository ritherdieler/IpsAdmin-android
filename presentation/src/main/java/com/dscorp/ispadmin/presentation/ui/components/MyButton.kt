package com.dscorp.ispadmin.presentation.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun MyButton(
    modifier: Modifier = Modifier, 
    text: String, 
    enabled: Boolean = true, 
    isLoading: Boolean = false,
    debounceTime: Long = 500L, // Tiempo de debounce en ms
    onClick: () -> Unit
) {
    // Estado para controlar el período de enfriamiento del botón
    var isClickable by remember { mutableStateOf(true) }
    
    Button(
        onClick = {
            if (isClickable) {
                onClick()
                isClickable = false
            }
        },
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
        enabled = enabled && !isLoading && isClickable
    ) {
        // Resetear isClickable después de debounceTime
        LaunchedEffect(key1 = isClickable) {
            if (!isClickable) {
                delay(debounceTime)
                isClickable = true
            }
        }
        
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(text)
            }
        }
    }
}
