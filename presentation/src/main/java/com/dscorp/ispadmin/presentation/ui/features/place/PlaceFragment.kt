package com.dscorp.ispadmin.presentation.ui.features.place

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.databinding.FragmentPlaceBinding
import com.dscorp.ispadmin.presentation.extension.navigateSafe
import com.dscorp.ispadmin.presentation.extension.showErrorDialog
import com.dscorp.ispadmin.presentation.extension.showSuccessDialog
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.Place
import com.google.android.gms.maps.model.LatLng
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlaceFragment() : Fragment() {
    private var selectedLocation: LatLng? = null
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ::onLocationPermissionResult
    )
    lateinit var binding: FragmentPlaceBinding
    val viewModel: PlaceViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_place, null, true)
        observePlaceResponse()
        observeFormError()
        observeCleanErrorForm()

        binding.btRegisterPlace.setOnClickListener {
            registerPlace()
        }
        binding.etLocation.setOnClickListener {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        observeMapDialogResult()
        return binding.root
    }

    private fun observeMapDialogResult() {
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<LatLng>("location")
            ?.observe(viewLifecycleOwner) {
                onLocationSelected(it)
            }
    }

    @SuppressLint("SetTextI18n")
    private fun onLocationSelected(it: LatLng) {
        this.selectedLocation = it
        binding.etLocation.setText("${it.latitude}, ${it.longitude}")
    }

    private fun registerPlace() {
        val placeObject = Place(
            abbreviation = binding.etAbbreviation.text.toString(),
            name = binding.etNamePlace.text.toString(),
            location = GeoLocation(
                selectedLocation?.latitude ?: 0.0,
                selectedLocation?.longitude ?: 0.0
            )
        )
        viewModel.registerPlace(placeObject)
    }

    private fun observeFormError() {
        viewModel.formErrorLiveData.observe(viewLifecycleOwner) { formError ->
            when (formError) {
                is FormError.OnEtAbbreviationError -> binding.tlAbbreviation.error =
                    formError.message

                is FormError.OnEtNameError -> binding.tlNamePlace.error = formError.message
                is FormError.OnEtLocationError -> binding.tlLocation.error = formError.message
                is FormError.OnEtNameIsInvalidError -> binding.tlNamePlace.error = formError.message
            }
        }
    }

    private fun observeCleanErrorForm() {
        viewModel.cleanErrorFormLiveData.observe(viewLifecycleOwner) { errorCleanForm ->
            when (errorCleanForm) {

                CleanFormErrorsPlace.OnEtAbbreviationCleanError -> binding.tlAbbreviation.error =
                    null

                CleanFormErrorsPlace.OnEtNamePlaceCleanError -> binding.tlNamePlace.error = null
            }
        }
    }

    private fun observePlaceResponse() {
        viewModel.placeResponseLiveData.observe(viewLifecycleOwner) { response ->
            when (response) {
                is PlaceResponse.OnError -> showErrorDialog()
                is PlaceResponse.OnPlaceRegister -> showSuccessDialog(response)
            }
        }
    }

    private fun showSuccessDialog(response: PlaceResponse.OnPlaceRegister) {
        showSuccessDialog(" ${response.place.name} Ah sido registrado con extio")
    }

    private fun onLocationPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            findNavController().navigateSafe(R.id.action_nav_register_place_to_mapDialog)
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showRationaleDialog()
            } else {
                Toast.makeText(
                    requireContext(),
                    "No se puede acceder a la ubicación",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showRationaleDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setMessage("Esta seccion necesita el permiso de ubicacion para funcionar correctamente.")
            .setPositiveButton("OK") { _, _ ->
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Handle user canceling the dialog
            }
        val dialog = builder.create()
        dialog.show()
    }

    private fun toast(s: String) {
        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show()
    }
}
