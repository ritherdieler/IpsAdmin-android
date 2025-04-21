package com.dscorp.ispadmin.presentation.ui.features.subscription.register

import NapBoxMapFragment.Companion.NAP_BOX_OBJECT
import NapBoxMapFragment.Companion.NAP_BOX_SELECTION_RESULT
import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.databinding.FragmentRegisterSubscriptionBinding
import com.dscorp.ispadmin.presentation.extension.analytics.AnalyticsConstants
import com.dscorp.ispadmin.presentation.extension.analytics.sendTouchButtonEvent
import com.dscorp.ispadmin.presentation.extension.animateRotate360InLoop
import com.dscorp.ispadmin.presentation.extension.getCurrentLocation
import com.dscorp.ispadmin.presentation.extension.openLocationSetting
import com.dscorp.ispadmin.presentation.extension.populate
import com.dscorp.ispadmin.presentation.extension.showCrossDialog
import com.dscorp.ispadmin.presentation.extension.showLocationRationaleDialog
import com.dscorp.ispadmin.presentation.extension.withGpsEnabled
import com.dscorp.ispadmin.presentation.ui.features.base.BaseFragment
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionFragmentDirections.actionNavSubscriptionToMapDialog
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionFragmentDirections.saveSubscriptionToNapBoxMapFragment
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.CouponIsValid
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.FiberDevicesFound
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.FormDataFound
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.OnOnuDataFound
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.PlansFound
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.RefreshingOnus
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.RegisterSubscriptionSuccess
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.ShimmerVisibility
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.WirelessDevicesFound
import com.dscorp.ispadmin.presentation.util.PermissionManager
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.extensions.toFormattedDateString
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Calendar
import java.util.TimeZone
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit

class RegisterSubscriptionFragment :
    BaseFragment<RegisterSubscriptionUiState, FragmentRegisterSubscriptionBinding>() {

    override val binding by lazy { FragmentRegisterSubscriptionBinding.inflate(layoutInflater) }
    override val viewModel: RegisterSubscriptionViewModel by viewModel()

    private lateinit var permissionManager: PermissionManager

    private val additionalDevicesAdapter by lazy {
        ArrayAdapter<NetworkDevice>(requireContext(), android.R.layout.simple_spinner_item)
    }

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun handleState(state: RegisterSubscriptionUiState) {
        when (state) {
            is RegisterSubscriptionSuccess -> showConfirmationDialog(state)
            is FormDataFound -> fillFormSpinners(state)
            is FiberDevicesFound -> fillCpeDeviceSpinner(state.devices)
            is WirelessDevicesFound -> fillCpeDeviceSpinner(state.devices)
            is CouponIsValid -> showCouponActivationResponse(state.isValid)
            is OnOnuDataFound -> populateOnuSpinner(state)
            is RefreshingOnus -> showOnusRefreshing(state.isRefreshing)
            is ShimmerVisibility -> showLoadingStatus(state.showShimmer)
            is PlansFound -> fillPlanSpinner(state.plans)
        }
    }

    private fun fillPlanSpinner(plans: List<PlanResponse>) {
        binding.etPlan.populate(plans) {
            viewModel.subscriptionForm.planField.liveData.value = it
        }
    }

    private fun getCurrentLocation() {
        binding.ivMyLocation.setImageResource(R.drawable.ic_rotate_right)
        binding.ivMyLocation.animateRotate360InLoop()
        fusedLocationClient.getCurrentLocation {
            binding.etLocationSubscription.setText("${it.latitude}, ${it.longitude}")
            viewModel.subscriptionForm.locationField.liveData.value = it
            binding.ivMyLocation.clearAnimation()
            binding.ivMyLocation.setImageResource(R.drawable.ic_my_location)
        }
    }


    override fun onViewReady(savedInstanceState: Bundle?) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.executePendingBindings()

        permissionManager = PermissionManager(
            this,
            onDeny = { openLocationSetting() },
            onRationale = {
                showLocationRationaleDialog()
            })

        observeNapBoxSelection()

        viewModel.getFormData()
        binding.ivRefresh.setOnClickListener {
            viewModel.getOnuData()
        }

        observeMapDialogResult()

        setInstallationTypeRadioGroupListener()
        binding.lvAditionalNetworkDevices.adapter = additionalDevicesAdapter
        binding.rgInstallationType.check(R.id.rbFiber)

        binding.ProgressButton.clickListener = {
            firebaseAnalytics.sendTouchButtonEvent(AnalyticsConstants.REGISTER_SUBSCRIPTION)
            viewModel.registerSubscription()
        }
        binding.ivMyLocation.setOnClickListener {
            withGpsEnabled {
                permissionManager.requestPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    onGranted = {
                        getCurrentLocation()
                    }
                )
            }
        }

        binding.etSubscriptionDate.setOnClickListener {
            showDatePicker()
        }

        binding.etLocationSubscription.setOnClickListener {
            withGpsEnabled {
                permissionManager.requestPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    onGranted = {
                        findNavController().navigate(actionNavSubscriptionToMapDialog())
                    }
                )
            }
        }

        binding.chkAdditionalDevices.setOnCheckedChangeListener { _, isChecked ->
            resetAdditionalDevicesUiState()
            if (isChecked) {
                binding.llAdditionalDevices.visibility = View.VISIBLE
            } else {
                binding.llAdditionalDevices.visibility = View.GONE
            }
            moveScrollViewToBottom()
        }

        binding.btnAddAditionalNetworkDevices.setOnClickListener {
            viewModel.addSelectedAdditionalNetworkDeviceToList()
            additionalDevicesAdapter.clear()
            additionalDevicesAdapter.addAll(viewModel.additionalNetworkDevicesList)
        }

        binding.acNapBox.setOnLongClickListener {
            withGpsEnabled {
                permissionManager.requestPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    onGranted = {
                        findNavController().navigate(saveSubscriptionToNapBoxMapFragment())
                    }
                )
            }
            true
        }

        val currentTimeMillis = System.currentTimeMillis()

        viewModel.subscriptionForm.subscriptionDateField.liveData.value = currentTimeMillis
        binding.etSubscriptionDate.setText(currentTimeMillis.toFormattedDateString())
        binding.etSubscriptionDate.isEnabled = false
        binding.etSubscriptionDate.visibility = View.GONE
    }

    private fun observeNapBoxSelection() {
        parentFragmentManager.setFragmentResultListener(
            NAP_BOX_SELECTION_RESULT, this
        ) { _, result ->
            val napBox = result.getSerializable(NAP_BOX_OBJECT) as NapBoxResponse
            viewModel.subscriptionForm.napBoxField.liveData.value = napBox
            binding.acNapBox.setText(napBox.toString())
        }
    }

    private fun setInstallationTypeRadioGroupListener() {
        binding.rgInstallationType.setOnCheckedChangeListener { _, checkedId ->
            resetCpeSpinner()
            resetNapBoxSpinner()

            resetAdditionalDevicesUiState()
            binding.tlAditonalNetworkDevices.visibility = View.VISIBLE
            binding.tlCpeNetworkDevice.visibility = View.VISIBLE
            binding.chkAdditionalDevices.visibility = View.VISIBLE
            resetPlanSpinner()

            when (checkedId) {
                R.id.rbFiber -> {
                    viewModel.installationType.value = InstallationType.FIBER
                    viewModel.getFiberDevices()
                    viewModel.getFiberPlans()
                    binding.spnNapBox.visibility = View.VISIBLE
                    binding.tlOnu.visibility = View.VISIBLE
                    binding.ivRefresh.visibility = View.VISIBLE
                }

                R.id.rbWireless -> {
                    viewModel.installationType.value = InstallationType.WIRELESS
                    viewModel.getWirelessDevices()
                    viewModel.getWirelessPlans()
                    binding.spnNapBox.visibility = View.GONE
                    binding.tlOnu.visibility = View.GONE
                    binding.ivRefresh.visibility = View.GONE
                }
            }
            moveScrollViewToBottom()
        }
    }

    private fun resetPlanSpinner() {
        viewModel.subscriptionForm.planField.liveData.value = null
        binding.etPlan.setText("")
    }

    private fun resetNapBoxSpinner() {
        viewModel.subscriptionForm.napBoxField.liveData.value = null
        binding.acNapBox.setText("")
    }

    private fun moveScrollViewToBottom() {
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun resetAdditionalDevicesUiState() {
        viewModel.resetAdditionalDevicesValues()
        additionalDevicesAdapter.clear()
        binding.acAditionalNetworkDevices.setText("")
    }

    private fun resetCpeSpinner() {
        viewModel.subscriptionForm.cpeDeviceField.liveData.value = null
        binding.etCpeNetworkDevice.setText("")
    }

    private fun observeMapDialogResult() {
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<LatLng>("location")
            ?.observe(viewLifecycleOwner) {
                onLocationSelected(it)
            }
    }

    @SuppressLint("SetTextI18n")
    private fun onLocationSelected(it: LatLng) {
        viewModel.subscriptionForm.locationField.liveData.value = it
        binding.etLocationSubscription.setText("${it.latitude}, ${it.longitude}")
    }

    private fun populateOnuSpinner(response: OnOnuDataFound) =
        binding.acOnu.populate(response.onus) {
            viewModel.subscriptionForm.onuField.liveData.value = it
        }

    private fun showOnusRefreshing(refreshing: Boolean) {
        if (refreshing) {
            binding.ivRefresh.isEnabled = false
            binding.ivRefresh.animateRotate360InLoop()
        } else {
            binding.ivRefresh.isEnabled = true
            binding.ivRefresh.clearAnimation()
        }
    }

    private fun showConfirmationDialog(response: RegisterSubscriptionSuccess) {
        showCrossDialog(
            getString(R.string.subscription_register_success, response.subscription.ip.toString()),
            closeButtonClickListener = { findNavController().navigate(R.id.register_payment) },
            positiveButtonClickListener = { findNavController().navigate(R.id.register_payment) }
        )
    }

    private fun showLoadingStatus(isLoading: Boolean) {
        binding.shimmerInclude.shimmerLayout.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.viewContainer.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showCouponActivationResponse(couponIsValid: Boolean) {
        if (couponIsValid) Toast.makeText(requireContext(), "Cupon valido", Toast.LENGTH_SHORT)
            .show()
        else Toast.makeText(requireContext(), "Cupon no valido", Toast.LENGTH_SHORT).show()
    }

    private fun fillCpeDeviceSpinner(devices: List<NetworkDevice>) {
        binding.etCpeNetworkDevice.populate(devices) {
            viewModel.subscriptionForm.cpeDeviceField.liveData.value = it
        }
    }

    private fun fillFormSpinners(response: FormDataFound) {

        binding.etPlace.populate(response.places) {
            viewModel.subscriptionForm.placeField.liveData.value = it
        }

        populateTechnicianSpinner(response)

        binding.acNapBox.populate(response.napBoxes) {
            viewModel.subscriptionForm.napBoxField.liveData.value = it
        }

        populateHostDeviceSpinner(response)

        binding.acAditionalNetworkDevices.populate(response.networkDevices) {
            viewModel.selectedAdditionalDevice.value = it
        }
        binding.acOnu.populate(response.unconfirmedOnus) {
            viewModel.subscriptionForm.onuField.liveData.value = it
        }
    }

    private fun populateTechnicianSpinner(response: FormDataFound) {
        binding.etTechnician.populate(response.technicians) {
            viewModel.subscriptionForm.technicianField.liveData.value = it
        }
        if (response.technicians.size == 1) {
            viewModel.subscriptionForm.technicianField.liveData.value = response.technicians[0]
            binding.etTechnician.setText(viewModel.subscriptionForm.technicianField.liveData.value.toString())
            binding.spnTechnician.isEnabled = false
            binding.spnTechnician.visibility = View.GONE
        }
    }

    private fun populateHostDeviceSpinner(response: FormDataFound) {
        binding.etHostDevice.populate(response.hostNetworkDevices) {
            viewModel.subscriptionForm.hostDeviceField.liveData.value = it
        }
        if (response.hostNetworkDevices.size == 1) {
            viewModel.subscriptionForm.hostDeviceField.liveData.value = response.hostNetworkDevices[0]
            binding.etHostDevice.setText(viewModel.subscriptionForm.hostDeviceField.liveData.value.toString())
            binding.spnHostDevice.isEnabled = false
            binding.spnHostDevice.visibility = View.GONE
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        val dateValidatorMin = DateValidatorPointForward.from(
            calendar.timeInMillis - 15.days.toLong(DurationUnit.MILLISECONDS)
        )

        val dateValidatorMax = DateValidatorPointBackward.before(calendar.timeInMillis)

        val dateValidator =
            CompositeDateValidator.allOf(listOf(dateValidatorMin, dateValidatorMax))

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setValidator(dateValidator)
                    .build()
            )
            .build()
        datePicker.addOnPositiveButtonClickListener {
            viewModel.subscriptionForm.subscriptionDateField.liveData.value = it
            val formattedDate = it.toFormattedDateString()
            binding.etSubscriptionDate.setText(formattedDate)
        }
        datePicker.show(childFragmentManager, "DatePicker")
    }

}

