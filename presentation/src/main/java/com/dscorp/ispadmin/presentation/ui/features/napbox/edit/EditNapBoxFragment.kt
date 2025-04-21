package com.dscorp.ispadmin.presentation.ui.features.napbox.edit

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.databinding.FragmentEditNapBoxBinding
import com.dscorp.ispadmin.presentation.extension.navigateSafe
import com.dscorp.ispadmin.presentation.extension.showErrorDialog
import com.dscorp.ispadmin.presentation.extension.showSuccessDialog
import com.dscorp.ispadmin.presentation.ui.features.napbox.NapBoxViewModel
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.NapBox
import com.google.android.gms.maps.model.LatLng
import org.koin.androidx.viewmodel.ext.android.viewModel

class EditNapBoxFragment : Fragment() {
    private val args by navArgs<EditNapBoxFragmentArgs>()
    private var selectedLocation: LatLng? = null
    lateinit var binding: FragmentEditNapBoxBinding
    val viewModel: NapBoxViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding =
            DataBindingUtil.inflate(layoutInflater, R.layout.fragment_edit_nap_box, null, true)
        viewModel.napBoxResponse = args.napBox
        fillFormWithSubscriptionData()
        observeNapBoxResponse()
        observeNapBoxFormError()

        binding.btnRegisterNapBox.setOnClickListener {
//            firebaseAnalytics.sendTouchButtonEvent(AnalyticsConstants.EDIT_NAP_BOX)
            editNapBox()
        }

        binding.etLocationNapBox.setOnClickListener {
            findNavController().navigateSafe(R.id.action_nav_to_register_nap_box_to_mapDialog)
        }
        observeMapDialogResult()

        return binding.root
    }

    private fun fillFormWithSubscriptionData() {
        binding.etCode.setText(viewModel.napBoxResponse?.code)
        binding.etAddress.setText(viewModel.napBoxResponse?.address)
        binding.etLocationNapBox.setText("${viewModel.napBoxResponse?.latitude}, ${viewModel.napBoxResponse?.longitude}")
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
        binding.etLocationNapBox.setText("${it.latitude}, ${it.longitude}")
    }

    private fun editNapBox() {
        val location = selectedLocation?.let { GeoLocation(it.latitude, it.longitude) }
        val registerNapBox = NapBox(
            code = binding.etCode.text.toString(),
            address = binding.etAddress.text.toString(),
            latitude = location?.latitude,
            longitude = location?.longitude,
            placeName = "placeName",
            placeId = -1,
        )
        viewModel.editNapBox(registerNapBox)
    }

    private fun observeNapBoxFormError() {
        viewModel.editFormErrorLiveData.observe(viewLifecycleOwner) { formError ->
            when (formError) {
                is EditNapBoxFormErrorUiState.OnEtAddressError -> binding.tlAddress.error =
                    formError.error

                is EditNapBoxFormErrorUiState.OnEtCodeError -> binding.tlCode.error =
                    formError.error

                is EditNapBoxFormErrorUiState.OnEtLocationError ->
                    binding.tlLocationNapBox.error =
                        formError.error

                is EditNapBoxFormErrorUiState.OnEtAddressCleanError -> binding.etAddress.error =
                    null

                is EditNapBoxFormErrorUiState.OnEtLocationCleanError -> binding.etAddress.error =
                    null

                is EditNapBoxFormErrorUiState.OnEtCodeCleanError -> binding.etAddress.error = null
            }
        }
    }

    private fun observeNapBoxResponse() {
        viewModel.editNapBoxUiState.observe(viewLifecycleOwner) { response ->
            when (response) {
                is EditNapBoxUiState.EditNapBoxError -> showErrorDialog(response.error)
                is EditNapBoxUiState.EditNapBoxSuccess -> showSuccessDialog("Nap box editado con exito")
                is EditNapBoxUiState.FetchFormDataError -> showErrorDialog(response.error)
            }
        }
    }
}
