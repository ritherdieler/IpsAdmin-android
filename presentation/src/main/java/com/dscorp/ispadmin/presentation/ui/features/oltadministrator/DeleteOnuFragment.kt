package com.dscorp.ispadmin.presentation.ui.features.oltadministrator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dscorp.ispadmin.databinding.FragmentDeleteOnuBinding
import com.dscorp.ispadmin.presentation.ui.features.migration.ErrorDialog
import com.dscorp.ispadmin.presentation.ui.features.migration.Loader
import com.example.data2.data.response.AdministrativeOnuResponse
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class DeleteOnuFragment : Fragment() {
    val viewModel: OltAdministrationViewModel by viewModel()
    val binding by lazy { FragmentDeleteOnuBinding.inflate(layoutInflater) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        lifecycleScope.launch {
            setComposableContent()
        }

        return binding.root
    }

    private fun setComposableContent() {
        binding.root.setContent {

            val uiState = viewModel.uiState.collectAsState()

            when (val state = uiState.value) {
                is OltAdministrationUiState.GetOnuSuccess -> DeleteOnuScreen(
                    onu = state.onu,
                    onSearchClick = {
                        viewModel.getOnuBySn(it)
                    },
                    onDeleteClick = {
                        viewModel.deleteOnuFromOlt(it)
                    }
                )

                is OltAdministrationUiState.Error -> ErrorDialog(
                    error = state.error,
                    onDismissRequest = {
                        viewModel.showForm()
                    }
                )

                is OltAdministrationUiState.Empty -> DeleteOnuScreen(
                    onSearchClick = {
                        viewModel.getOnuBySn(it)
                    },
                    onDeleteClick = {
                        viewModel.deleteOnuFromOlt(it)
                    }
                )

                is OltAdministrationUiState.Loading -> {
                    Loader()
                }

                OltAdministrationUiState.DeleteOnuSuccess -> {
                    Toast.makeText(requireContext(), "Onu eliminada correctamente", Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
            }

        }

    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun DeleteOnuScreen(
        onu: AdministrativeOnuResponse? = null,
        onSearchClick: (String) -> Unit,
        onDeleteClick: (String) -> Unit
    ) {

        var searchText by remember { mutableStateOf("") }
        val keyboardController = LocalSoftwareKeyboardController.current

        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = searchText,
                onValueChange = { searchText = it.uppercase() },
                label = { Text("Buscar por por SN") },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearchClick(searchText)
                        keyboardController?.hide()
                    }
                )
            )
            onu?.let {
                Spacer(Modifier.height(16.dp))
                OnuCard(administrativeOnuResponse = it, onDeleteClick = { externalOnuId ->
                    onDeleteClick(externalOnuId)
                })
            }

        }
    }

    @Composable
    fun OnuCard(
        modifier: Modifier = Modifier,
        administrativeOnuResponse: AdministrativeOnuResponse,
        onDeleteClick: (String) -> Unit = {}
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "SN: ${administrativeOnuResponse.sn}")
                Text(text = "Nombre: ${administrativeOnuResponse.name.uppercase()}")
                Text(text = "Service Port: ${administrativeOnuResponse.mgmt_ip_service_port}")
                Text(text = "Fecha de autorizacion: ${administrativeOnuResponse.authorization_date}")
                Text(text = "Perfil: ${administrativeOnuResponse.custom_template_name}")
                Text(text = "ONU Type Name: ${administrativeOnuResponse.onu_type_name}")
                Text(text = "ONU: ${administrativeOnuResponse.onu}")
                Text(text = "Port: ${administrativeOnuResponse.port}")
                Text(text = "Board: ${administrativeOnuResponse.board}")

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = {
                        onDeleteClick(administrativeOnuResponse.unique_external_id)
                    }) {
                        Text("Eliminar Onu")
                    }
                }

            }
        }
    }

    @Preview
    @Composable
    fun OnuScreenPreview() {
        OnuCard(
            administrativeOnuResponse = AdministrativeOnuResponse(
                sn = "quaerendum",
                name = "Carmen Hines",
                mgmt_ip_service_port = "convenire",
                authorization_date = "cu",
                custom_template_name = "Mark Fry",
                onu_type_name = "Hollis Fernandez",
                onu = "duo",
                port = "auctor",
                board = "graece",
                unique_external_id = "leo"
            ), modifier = Modifier
        )
    }

}