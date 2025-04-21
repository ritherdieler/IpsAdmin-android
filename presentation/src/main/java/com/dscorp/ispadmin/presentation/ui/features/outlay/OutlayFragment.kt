package com.dscorp.ispadmin.presentation.ui.features.outlay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.dscorp.ispadmin.databinding.FragmentRegisterOutlayBinding
import com.dscorp.ispadmin.presentation.ui.features.dialog.MyConfirmDialog
import com.dscorp.ispadmin.presentation.ui.features.migration.Loader
import com.dscorp.ispadmin.domain.model.Outlay
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.viewmodel.ext.android.viewModel

class RegisterOutLaysFragment : Fragment() {

    private val binding by lazy { FragmentRegisterOutlayBinding.inflate(layoutInflater) }
    private val viewModel: OutLayViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding.root.setContent {
            RegisterOutlayScreen(viewModel.uiState)
        }

        return binding.root
    }


    @Composable
    fun RegisterOutlayScreen(uiState: StateFlow<OutlayUiState>) {


        val lifeCycle = LocalLifecycleOwner.current.lifecycle

        val mUiState by produceState<OutlayUiState>(initialValue = OutlayUiState.Idle) {

            lifeCycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                uiState.collect { value = it }
            }
        }

        if (mUiState is OutlayUiState.Loading) Loader()
        else RegisterOutLayForm(
            onRegisterClick = { amount, description, code ->

                val outLay = Outlay(
                    amount = amount.toDouble(),
                    description = description,
                    document_code = code
                )

                viewModel.registerOutLay(outLay)

            },
            onBackClick = { }
        )


        if (mUiState is OutlayUiState.Error)
            MyConfirmDialog(title = "Error", body = {
                Text(text = "Error al registrar el egreso")
            }, onDismissRequest = {
                viewModel.updateState(OutlayUiState.Idle)
            })


        if (mUiState is OutlayUiState.Saved)
            MyConfirmDialog(title = "Éxito", body = {
                Text(text = "Egreso registrado correctamente")
            }, onDismissRequest = {
                viewModel.updateState(OutlayUiState.Idle)
            })
    }

    @Composable
    fun RegisterOutLayForm(
        modifier: Modifier = Modifier,
        onRegisterClick: (amount: String, description: String, code: String) -> Unit,
        onBackClick: () -> Unit = {},
    ) {
        var amount by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }

        Column(
            modifier = modifier.padding(vertical = 32.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = amount,
                trailingIcon = {
                    IconButton(onClick = {
                        amount = ""
                    }) {
                        Icon(imageVector = Icons.Filled.Clear, contentDescription = "clear amount")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text(text = "Monto") },
                onValueChange = {
                    if (it.contains(",") || it.contains(" ") || it.contains("-") || it.contains("\n") || it.length > 5) return@OutlinedTextField
                    else amount = it
                })
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Descripción") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                trailingIcon = {
                    IconButton(onClick = {
                        description = ""
                    }) {
                        Icon(imageVector = Icons.Filled.Clear, contentDescription = "clear amount")
                    }
                },
                value = description,
                onValueChange = {
                    if (it.length > 200) return@OutlinedTextField

                    description = it
                }
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = code,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                trailingIcon = {
                    IconButton(onClick = {
                        code = ""
                    }) {
                        Icon(imageVector = Icons.Filled.Clear, contentDescription = "clear amount")
                    }
                },
                label = { Text(text = "Código") },
                onValueChange = {
                    if (it.length > 50) return@OutlinedTextField
                    code = it
                }
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            )



            Button(enabled = amount.isNotEmpty() && description.isNotEmpty(),
                onClick = {
                    onRegisterClick(amount, description, code)
                }) {
                Text(text = "Registrar egreso")
            }
        }

    }


    @Preview(showBackground = true, showSystemUi = true)
    @Composable
    fun RegisterOutLayScreenPreview() {
        RegisterOutLayForm(
            modifier = Modifier,
            onRegisterClick = { d: String, s: String, s1: String -> },
            onBackClick = {})
    }
}




