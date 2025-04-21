package com.dscorp.ispadmin.presentation.ui.features.napbox.register

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.databinding.FragmentNapBoxBinding
import com.dscorp.ispadmin.presentation.extension.navigateSafe
import com.dscorp.ispadmin.presentation.extension.populate
import com.dscorp.ispadmin.presentation.extension.showErrorDialog
import com.dscorp.ispadmin.presentation.extension.showSuccessDialog
import com.dscorp.ispadmin.presentation.ui.features.napbox.NapBoxViewModel
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.Mufa
import com.google.android.gms.maps.model.LatLng
import org.koin.androidx.viewmodel.ext.android.viewModel

class NapBoxFragment : Fragment() {
    val binding by lazy { FragmentNapBoxBinding.inflate(layoutInflater) }
    val viewModel: NapBoxViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.executePendingBindings()

        addTextChangeListeners()
        observeNapBoxResponse()

        viewModel.getMufas()
        binding.btRegisterNapBox.setOnClickListener {
            viewModel.registerNapBox()
        }

        binding.etLocationNapBox.setOnClickListener {
            findNavController().navigateSafe(R.id.action_nav_to_register_nap_box_to_mapDialog)
        }
        observeMapDialogResult()

        return binding.root
    }

    private fun addTextChangeListeners() {
        binding.etCode.doOnTextChanged { text, start, before, count ->
            viewModel.codeField.value = text.toString()
        }

        binding.etAddress.doOnTextChanged { text, start, before, count ->
            viewModel.addressField.value = text.toString()
        }
    }

    private fun observeMapDialogResult() {
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<LatLng>("location")
            ?.observe(viewLifecycleOwner) {
                onLocationSelected(it)
            }
    }

    @SuppressLint("SetTextI18n")
    private fun onLocationSelected(it: LatLng) {
        viewModel.locationField.value = GeoLocation(it.latitude, it.longitude)
        binding.etLocationNapBox.setText("${it.latitude}, ${it.longitude}")
    }

    private fun observeNapBoxResponse() {
        viewModel.uiState.observe(viewLifecycleOwner) { response ->
            when (response) {
                is RegisterNapBoxUiState.OnError -> showErrorDialog()
                is RegisterNapBoxUiState.OnRegisterNapBoxSealedClassRegister -> showSuccessDialog(
                    response
                )

                is RegisterNapBoxUiState.MufasReady -> fillNapSpinner(response.mufas)
            }
        }
    }

    private fun fillNapSpinner(mufas: List<Mufa>) {
        binding.acMufa.populate(mufas) {
            viewModel.mufaField.value = it
        }
    }

    private fun showSuccessDialog(response: RegisterNapBoxUiState.OnRegisterNapBoxSealedClassRegister) {
        showSuccessDialog("La Caja Nap ${response.napBox.code} Ah Sido Registrado Correctamente.")
    }
}