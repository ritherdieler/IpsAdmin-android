package com.dscorp.ispadmin.presentation.ui.components

import android.R
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout

/**
 * Un componente Composable que envuelve MaterialAutoCompleteTextView para usar en Jetpack Compose.
 *
 * @param items Lista genérica de elementos para mostrar en el dropdown
 * @param label Etiqueta que se mostrará encima del campo
 * @param selectedItem Elemento seleccionado actualmente
 * @param onItemSelected Callback invocado cuando se selecciona un elemento
 * @param onSelectionCleared Callback invocado cuando se borra la selección
 * @param modifier Modificador para aplicar al componente
 * @param hint Texto de sugerencia cuando no hay selección
 * @param enabled Habilita o deshabilita el control
 */
@Composable
fun <T> MyAutoCompleteTextViewCompose(
    modifier: Modifier = Modifier,
    items: List<T>,
    label: String,
    selectedItem: T? = null,
    onItemSelected: (T) -> Unit,
    onSelectionCleared: () -> Unit,
    onTextChanged: (String) -> Unit = {},
    errorMessage: String? = null,
    enabled: Boolean = true,
    hasError: Boolean = false
) {
    val context = LocalContext.current
    var internalSelectedItem by remember { mutableStateOf(selectedItem) }
    var ignoreNextTextChange by remember { mutableStateOf(false) }

    val currentItems = items
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val fontSize = MaterialTheme.typography.bodyLarge.fontSize.value

    AndroidView(
        modifier = modifier,
        factory = { originalContext ->
            // Creamos el TextInputLayout con estilo outline
            val textInputLayout = TextInputLayout(originalContext).apply {
                hint = label
                isEnabled = enabled
                isErrorEnabled = hasError
                errorMessage?.let { error = errorMessage }
                // Modo de caja Outline
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                boxStrokeColor = primaryColor.toArgb()
                boxBackgroundColor = surfaceColor.toArgb()
                hintTextColor = ColorStateList.valueOf(onSurfaceVariant.toArgb())
                boxStrokeWidth = 2
                boxStrokeWidthFocused = 3

                // Configuración del ícono para desplegar el menú:
                endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // MaterialAutoCompleteTextView
            val autoCompleteTextView = MaterialAutoCompleteTextView(originalContext).apply {
                id = R.id.text1
                background = null
                isSingleLine = true
                threshold = 0 // Permite mostrar sugerencias incluso sin escribir
                setTextColor(onSurfaceColor.toArgb())
                setHintTextColor(onSurfaceVariant.toArgb())
                textSize = fontSize
                typeface = Typeface.DEFAULT
                setPadding(32, 20, 32, 20) // Ajuste similar a OutlinedTextField

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    originalContext.resources.displayMetrics.density.times(56).toInt() // 56dp
                )

                // Configura el adaptador de sugerencias
                setupAdapter(originalContext, currentItems)

                // Texto inicial si hay un item seleccionado
                internalSelectedItem?.let {
                    ignoreNextTextChange = true
                    setText(it.toString(), false)
                    setSelection(text.length)
                }

                // Escucha cambios de texto
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: Editable?) {
                        if (ignoreNextTextChange) {
                            ignoreNextTextChange = false
                            return
                        }

                        val currentText = s?.toString() ?: ""
                        val selectedText = internalSelectedItem?.toString() ?: ""

                        if (currentText != selectedText) {
                            internalSelectedItem = null
                            onSelectionCleared()
                        }

                        s?.let {
                            onTextChanged(it.toString())
                        }
                    }
                })

            }

            // Añadimos el AutoCompleteTextView al TextInputLayout
            textInputLayout.addView(autoCompleteTextView)
            textInputLayout
        },
        update = { textInputLayout ->
            textInputLayout.isEnabled = enabled
            errorMessage?.let { textInputLayout.error = errorMessage }
            val autoCompleteTextView =
                textInputLayout.findViewById<MaterialAutoCompleteTextView>(R.id.text1)
            textInputLayout.isErrorEnabled = hasError
            // Configurar un adaptador personalizado que nos permita acceder a los elementos filtrados
            val adapter = CustomItemAdapter(
                context,
                R.layout.simple_dropdown_item_1line,
                currentItems
            )
            autoCompleteTextView.setAdapter(adapter)

            // Listener para selección de ítem del dropdown
            autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
                // Obtenemos el elemento seleccionado del adaptador (que contiene la lista filtrada)
                val selectedValue = adapter.getItem(position)
                if (selectedValue != null) {
                    internalSelectedItem = selectedValue
                    ignoreNextTextChange = true
                    val selectedText = selectedValue.toString()
                    autoCompleteTextView.setText(selectedText, false)
                    autoCompleteTextView.setSelection(selectedText.length)
                    onItemSelected(selectedValue)

                    // Ocultar el teclado después de la selección
                    hideKeyboard(context, autoCompleteTextView)
                }
            }

            // Si el elemento seleccionado cambió externamente, actualizamos el texto
            if (internalSelectedItem != selectedItem) {
                internalSelectedItem = selectedItem
                ignoreNextTextChange = true
                val newText = selectedItem?.toString() ?: ""
                autoCompleteTextView.setText(newText, false)
                autoCompleteTextView.setSelection(newText.length)
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose { }
    }
}

// Adaptador personalizado para mantener los elementos filtrados
private class CustomItemAdapter<T>(
    context: Context,
    resource: Int,
    objects: List<T>
) : ArrayAdapter<T>(context, resource, objects) {
    override fun getItem(position: Int): T? {
        return super.getItem(position)
    }
}

// Nueva función compatible con el adaptador personalizado
private fun <T> MaterialAutoCompleteTextView.setupAdapter(context: Context, items: List<T>) {
    val adapter = CustomItemAdapter(
        context,
        R.layout.simple_dropdown_item_1line,
        items
    )
    setAdapter(adapter)
}

// Función para ocultar el teclado
private fun hideKeyboard(context: Context, view: View) {
    val inputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

@Preview
@Composable
private fun AtoCompletePreview() {
    MyTheme {
        var selected by remember { mutableStateOf<String?>(null) }
        Column {
            MyAutoCompleteTextViewCompose(
                items = listOf("Opción 1", "Opción 2", "Opción 3"),
                label = "Seleccione una opción",
                selectedItem = selected,
                onItemSelected = { selected = it },
                onSelectionCleared = { selected = null },
            )
            Text("Elemento seleccionado: ${selected ?: "Ninguno"}")
        }
    }
}
