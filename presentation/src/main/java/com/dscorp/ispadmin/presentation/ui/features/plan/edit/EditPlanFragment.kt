package com.dscorp.ispadmin.presentation.ui.features.plan.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.databinding.FragmentEditPlanBinding
import com.dscorp.ispadmin.presentation.extension.showCrossDialog
import com.dscorp.ispadmin.presentation.extension.showErrorDialog
import com.dscorp.ispadmin.domain.model.PlanResponse
import org.koin.androidx.viewmodel.ext.android.viewModel

class EditPlanFragment : Fragment() {

    private val args: EditPlanFragmentArgs by navArgs()
    private val viewModel by viewModel<EditPlanViewModel>()
    private val binding by lazy { FragmentEditPlanBinding.inflate(layoutInflater) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.setInitialPlanData(args.plan)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.executePendingBindings()
        observer()
        binding.btnEditPlan.clickListener = {
            viewModel.editPlan()
        }
        return binding.root
    }

    private fun observer() {
        viewModel.uiState.observe(viewLifecycleOwner) {
            it.error?.let { showErrorDialog(it.message) }
            it.loading?.let { binding.btnEditPlan.isLoading = it }
            it.uiState?.let { state ->
                when (state) {
                    is EditPlanUiState.EditPlanUpdateSuccess -> onEditPlanSuccess(state.plan)
                }
            }
        }
    }

    private fun onEditPlanSuccess(plan: PlanResponse) {
        showCrossDialog(getString(R.string.plan_edit_successful)) {
            findNavController().navigate(R.id.nav_dashboard)
        }
    }

}