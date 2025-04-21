package com.dscorp.ispadmin.presentation.ui.features.plan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.databinding.FragmentPlanBinding
import com.dscorp.ispadmin.presentation.extension.showErrorDialog
import com.dscorp.ispadmin.presentation.extension.showSuccessDialog
import com.dscorp.ispadmin.presentation.ui.features.plan.PlanFormError.*
import com.dscorp.ispadmin.domain.model.Plan
import org.koin.androidx.viewmodel.ext.android.viewModel

class RegisterPlanFragment : Fragment() {
    lateinit var binding: FragmentPlanBinding
    val viewModel: PlanViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_plan, null, true)
        observePlanResponse()
        observeFormError()
        observeErrorCleanForm()

        binding.btRegisterPlan.setOnClickListener {
            registerPlan()
        }

        return binding.root
    }

    private fun observePlanResponse() {
        viewModel.planResponseLiveData.observe(viewLifecycleOwner) { response ->
            when (response) {
                is PlanResponse.OnError -> showErrorDialog()
                is PlanResponse.OnPlanRegistered -> showSuccessDialog(response)
            }
        }
    }

    private fun showSuccessDialog(response: PlanResponse.OnPlanRegistered) {
        showSuccessDialog("El plan ${response.plan.name} ah sido registrado correctamente")
    }

    private fun observeFormError() {
        viewModel.formErrorLiveData.observe(viewLifecycleOwner) { formError ->
            when (formError) {
                is OnEtDowloadSpeedError -> setErrorEtDownloadSpeed(formError)
                is OnEtNamePlanError -> setErrorEtNamePlan(formError)
                is OnEtPriceError -> setErrorEtPrice(formError)
                is OnEtUploadSpeedError -> setErrorEtUploadSpeed(formError)
                is OnEtNameIsInvalidError -> binding.tlNamePlan.error = formError.message
            }
        }
    }

    private fun setErrorEtUploadSpeed(formError: OnEtUploadSpeedError) {
        binding.tlUploadSpeed.error = (formError.message)
    }

    private fun setErrorEtPrice(formError: OnEtPriceError) {
        binding.tlPrice.error = formError.message
    }

    private fun setErrorEtNamePlan(formError: OnEtNamePlanError) {
        binding.tlNamePlan.error = formError.message
    }

    private fun setErrorEtDownloadSpeed(formError: OnEtDowloadSpeedError) {
        binding.tlDownloadSpeed.error = formError.message
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun observeErrorCleanForm() {
        viewModel.errorCleanFormLiveData.observe(viewLifecycleOwner) { errorCleanForm ->
            when (errorCleanForm) {
                PlanErrorCleanForm.OnEtDownloadSpeedHasNotError ->
                    binding.tlDownloadSpeed.error =
                        null
                PlanErrorCleanForm.OnEtNamePlanHasNotError -> binding.tlNamePlan.error = null
                PlanErrorCleanForm.OnEtPriceHasNotError -> binding.tlPrice.error = null
                PlanErrorCleanForm.OnEtUploadSpeedHasNotError -> binding.tlUploadSpeed.error = null
            }
        }
    }

    private fun registerPlan() {

        val price =
            if (binding.etPrice.text.toString().isEmpty()) 0.0 else binding.etPrice.text.toString()
                .toDouble()
        val plan = Plan(
            name = binding.etNamePlan.text.toString(),
            price = price,
            downloadSpeed = binding.etDownloadSpeed.text.toString(),
            uploadSpeed = binding.etUploadSpeed.text.toString()
        )
        viewModel.registerPlan(plan)
    }
}
