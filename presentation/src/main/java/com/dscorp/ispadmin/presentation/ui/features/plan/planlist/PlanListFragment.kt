package com.dscorp.ispadmin.presentation.ui.features.plan.planlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.databinding.FragmentPlanListBinding
import com.dscorp.ispadmin.presentation.extension.showCrossDialog
import com.dscorp.ispadmin.domain.model.PlanResponse
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlanListFragment : Fragment(), OnPlanSelectedListener {

    private val binding by lazy { FragmentPlanListBinding.inflate(layoutInflater) }
    private val viewModel: PlanListViewModel by viewModel()
    private val planAdapter by lazy { PlanAdapter(this) }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding.adapter = planAdapter
        binding.executePendingBindings()
        observe()
        viewModel.getPlans()
        return binding.root
    }

    private fun observe() {

        viewModel.uiState.observe(viewLifecycleOwner) {
            it.error?.let { error -> showCrossDialog(error.message) }
            it.loading?.let { isLoading -> {} }
            it?.uiState?.let { state ->
                when (state) {
                    is PlanListUiState.OnPlanListFound -> planAdapter.submitList(state.planList)
                }
            }
        }
    }

    private fun showPopupMenu(view: View, plan: PlanResponse) {
        val popupMenu = PopupMenu(requireContext(), view).apply {
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.btn_edit_plan -> {
                        PlanListFragmentDirections.toEditPlan(plan).also { action ->
                            findNavController().navigate(action)
                        }
                        false
                    }
                    else -> false
                }
            }
            inflate(R.menu.plan_menu)
        }
        popupMenu.show()
    }

    override fun onPlanSelected(plan: PlanResponse, view: View) {
        showPopupMenu(view, plan)
    }

}

interface OnPlanSelectedListener {
    fun onPlanSelected(plan: PlanResponse, view: View)
}

