package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder
//
//import android.content.Intent
//import android.os.Bundle
//import android.view.View
//import android.widget.PopupMenu
//import androidx.appcompat.app.AlertDialog
//import androidx.navigation.Navigation
//import androidx.navigation.fragment.findNavController
//import com.dscorp.ispadmin.R
//import com.dscorp.ispadmin.databinding.FragmentFindSubscriptionBinding
//import com.dscorp.ispadmin.presentation.extension.showSuccessDialog
//import com.dscorp.ispadmin.presentation.ui.features.base.BaseFragment
//import com.dscorp.ispadmin.presentation.ui.features.migration.MigrationActivity
//import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.FindSubscriptionUiState.OnSubscriptionFound
//import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.FindSubscriptionUiState.PaymentCommitmentSuccess
//import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.FindSubscriptionUiState.ReactivateServiceSuccess
//import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.FindSubscriptionUiState.ShowEditPlanOption
//import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.FindSubscriptionUiState.ShowPaymentCommitmentOption
//import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.FindSubscriptionUiState.ShowReactivateServiceOption
//import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.FindSubscriptionUiState.ShowRegisterServiceOrder
//import com.example.cleanarchitecture.domain.domain.entity.SubscriptionResponse
//import com.example.cleanarchitecture.domain.domain.entity.extensions.localToUTC
//import com.example.cleanarchitecture.domain.domain.entity.extensions.toFormattedDateString
//import com.google.android.material.datepicker.MaterialDatePicker
//import org.koin.androidx.viewmodel.ext.android.viewModel
//
//class FindSubscriptionFragment :
//    BaseFragment<FindSubscriptionUiState, FragmentFindSubscriptionBinding>(),
//    SelectableSubscriptionListener {
//
//    override val binding: FragmentFindSubscriptionBinding by lazy {
//        FragmentFindSubscriptionBinding.inflate(layoutInflater)
//    }
//    private val adapter = FindSubscriptionAdapter(this)
//    private lateinit var popupMenu: PopupMenu
//    override val viewModel: FindSubscriptionViewModel by viewModel()
//
//    override fun handleState(state: FindSubscriptionUiState) {
//        when (state) {
//            is OnSubscriptionFound -> adapter.submitList(state.subscriptions)
//            is PaymentCommitmentSuccess -> showSuccessDialog(getString(R.string.payment_commitment_save_success))
//            is ShowPaymentCommitmentOption -> {
//                popupMenu.menu.findItem(R.id.btn_register_payment_commitment).isVisible =
//                    state.showOption
//            }
//
//            is ShowReactivateServiceOption -> {
//                popupMenu.menu.findItem(R.id.btn_reactivate_service).isVisible =
//                    state.showOption
//            }
//
//            is ShowEditPlanOption -> {
//                popupMenu.menu.findItem(R.id.btn_edit_plan_subscription).isVisible =
//                    state.showOption
//            }
//
//            is ShowRegisterServiceOrder -> {
//                popupMenu.menu.findItem(R.id.btn_register_service_order).isVisible =
//                    state.showOption
//            }
//
//            ReactivateServiceSuccess -> showSuccessDialog(getString(R.string.service_reactivated_successfully))
//            FindSubscriptionUiState.CancelSubscriptionSuccess -> {
//                adapter.submitList(emptyList())
//                showSuccessDialog(getString(R.string.service_cancelled_successfully))
//            }
//
//            FindSubscriptionUiState.ShowMigrationOption -> {
//                popupMenu.menu.findItem(R.id.btn_migrate_to_fiber).isVisible = true
//            }
//        }
//    }
//
//    override fun onViewReady(savedInstanceState: Bundle?) {
//        binding.viewModel = viewModel
//        binding.lifecycleOwner = this
//        binding.executePendingBindings()
//        binding.findSubscriptionRecyclerView.adapter = adapter
//
//        binding.etStartDate.setOnClickListener {
//            showStartDatePickerDialog()
//        }
//        binding.etEndDate.setOnClickListener {
//            showEndDatePickerDialog()
//        }
//
//        //ime options listener
//        binding.etDni.setOnEditorActionListener { _, actionId, _ ->
//            viewModel.findSubscriptionByDni()
//            true
//        }
//
//        binding.etFirstName.setOnEditorActionListener { _, actionId, _ ->
//            viewModel.findSubscriptionByNameAndLastName()
//            true
//        }
//
//
//        binding.etLastName.setOnEditorActionListener { _, actionId, _ ->
//            viewModel.findSubscriptionByNameAndLastName()
//            true
//        }
//        viewModel.loadingUiState.observe(viewLifecycleOwner) {
//            binding.pbarFindSubscription.visibility = if (it) View.VISIBLE else View.GONE
//        }
//    }
//
//    private fun showStartDatePickerDialog() {
//        val datePicker = MaterialDatePicker.Builder.datePicker()
//            .setTitleText("Select date")
//            .build()
//
//        datePicker.addOnPositiveButtonClickListener {
//            viewModel.startDateField.value = it.localToUTC()
//            viewModel.startDateField.value?.let { date ->
//                binding.etStartDate.setText(date.toFormattedDateString())
//            }
//        }
//        datePicker.show(childFragmentManager, "DatePickerStart")
//    }
//
//
//    private fun showEndDatePickerDialog() {
//        val datePicker = MaterialDatePicker.Builder.datePicker()
//            .setTitleText("Select date")
//            .build()
//        datePicker.addOnPositiveButtonClickListener {
//            viewModel.endDateField.value = it.localToUTC()
//            viewModel.endDateField.value?.let { date ->
//                binding.etEndDate.setText(date.toFormattedDateString())
//            }
//        }
//        datePicker.show(childFragmentManager, "DatePickerend")
//    }
//
//    override fun onSubscriptionPopupButtonSelected(subscription: SubscriptionResponse, view: View) {
//        showPopupMenu(view, subscription)
//    }
//
//    private fun navigateToEditSubscription(subscription: SubscriptionResponse): Boolean {
//        findNavController().navigate(
//            FindSubscriptionFragmentDirections.actionNavFindSubscriptionsToEditSubscriptionFragment(
//                subscription
//            )
//        )
//        return true
//    }
//
//    private fun showPopupMenu(view: View, subscription: SubscriptionResponse) {
//        popupMenu = PopupMenu(requireContext(), view)
//        popupMenu.setOnMenuItemClickListener {
//            when (it.itemId) {
//                R.id.btn_show_payment_history -> navigateToPaymentHistory(subscription)
//                R.id.btn_register_payment_commitment -> showPaymentCommitmentDialog(subscription)
//                R.id.btn_edit_plan_subscription -> navigateToEditSubscription(subscription)
//                R.id.btn_see_details -> navigateToDetails(subscription)
//                R.id.btn_reactivate_service -> showReactivateServiceDialog(subscription)
//                R.id.btn_cancel_subscription -> showCancelSubscriptionDialog(subscription)
//                R.id.btn_migrate_to_fiber -> showMigrationActivity(subscription)
//
//                else -> false
//            }
//        }
//        popupMenu.inflate(R.menu.subscription_menu)
//        popupMenu.menu.findItem(R.id.btn_register_service_order).isVisible = false
//        viewModel.filterMenuItems(subscription)
//        popupMenu.show()
//    }
//
//    private fun showMigrationActivity(subscription: SubscriptionResponse): Boolean {
//        val intent = Intent(requireContext(), MigrationActivity::class.java)
//        intent.putExtra("subscriptionResponse", subscription)
//        startActivity(intent)
//
//        return true
//    }
//
//    private fun showCancelSubscriptionDialog(subscription: SubscriptionResponse): Boolean {
//        AlertDialog.Builder(requireContext())
//            .setTitle(R.string.cancel_subscription)
//            .setMessage(R.string.cancel_subscription_message)
//            .setPositiveButton(R.string.yes) { p0, p1 ->
//                viewModel.cancelSubscription(subscription)
//            }
//            .setNegativeButton(R.string.cancel) { p0, p1 ->
//                p0.dismiss()
//            }.show()
//        return true
//    }
//
//    private fun showReactivateServiceDialog(subscription: SubscriptionResponse): Boolean {
//
//        AlertDialog.Builder(requireContext())
//            .setTitle(R.string.reactivate_service)
//            .setMessage(R.string.reactivate_service_message)
//            .setPositiveButton(R.string.yes) { p0, p1 ->
//                viewModel.reactivateService(subscription)
//            }
//            .setNegativeButton(R.string.cancel) { p0, p1 ->
//                p0.dismiss()
//            }.show()
//        return true
//
//    }
//
//    private fun showPaymentCommitmentDialog(subscription: SubscriptionResponse): Boolean {
//        AlertDialog.Builder(requireContext())
//            .setTitle(R.string.register_payment_commitment)
//            .setMessage(R.string.register_payment_commitment_message)
//            .setPositiveButton(R.string.yes) { p0, p1 ->
//                viewModel.savePaymentCommitment(subscription)
//            }
//            .setNegativeButton(R.string.cancel) { p0, p1 ->
//                p0.dismiss()
//            }.show()
//
//        return true
//    }
//
//    private fun navigateToDetails(subscription: SubscriptionResponse): Boolean {
//        val destination =
//            FindSubscriptionFragmentDirections.findSubscriptionToSubscriptionDetail(subscription)
//        findNavController().navigate(destination)
//        return true
//    }
//
//    private fun navigateToPaymentHistory(subscription: SubscriptionResponse): Boolean {
//        val action = FindSubscriptionFragmentDirections.findSubscriptionToPaymentHistoryFragment(
//            subscription
//        )
//        Navigation.findNavController(requireView()).navigate(action)
//        return true
//    }
//
//    override fun onResume() {
//        super.onResume()
//        clearFormData()
//    }
//
//    private fun clearFormData() {
//        adapter.submitList(emptyList())
//        binding.etLastName.text = null
//        binding.etFirstName.text = null
//        binding.etStartDate.text = null
//        binding.etEndDate.text = null
//        binding.etEndDate.text = null
//    }
//
//}
