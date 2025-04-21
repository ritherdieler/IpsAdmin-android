package com.dscorp.ispadmin.presentation.ui.features.napboxeslist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.databinding.FragmentNapBoxesListBinding
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class NapBoxesListFragment : Fragment(), OnItemClickListener {

    private lateinit var binding: FragmentNapBoxesListBinding
    private val viewModel: NapBoxesListViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(
            layoutInflater, R.layout.fragment_nap_boxes_list, null,
            true
        )
        observe()
        return binding.root
    }

    override fun onNapBoxPopupButtonSelected(napBox: NapBoxResponse, view: View) {
        showPopupMenu(view, napBox)
    }

    private fun showPopupMenu(view: View, napBox: NapBoxResponse) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.btn_edit_nap_box -> navigateToEditSubscription(napBox)

                else -> false
            }
        }
        popupMenu.inflate(R.menu.nap_box_menu)
        popupMenu.show()
    }

    private fun
            navigateToEditSubscription(napBox: NapBoxResponse): Boolean {
//        findNavController().navigate(
//            NapBoxesListFragmentDirections.actionNavSeeNapBoxesToEditNapBoxFragment(
//                napBox
//            )
//        )
        return true
    }

    private fun observe() {
        lifecycleScope.launch {
            viewModel.responseLiveData.observe(viewLifecycleOwner) {
                when (it) {
                    is NapBoxesListResponse.OnError -> {}
                    is NapBoxesListResponse.OnNapBoxesListFound -> fillRecycleView(it)
                }
            }
        }
    }

    private fun fillRecycleView(it: NapBoxesListResponse.OnNapBoxesListFound) {
        val adapter = NapBoxeAdapter(this)
        adapter.submitList(it.napBoxesList)
        binding.rvNapBoxesList.adapter = adapter

        binding.rvNapBoxesList.visibility =
            if (it.napBoxesList.isNotEmpty()) View.VISIBLE else View.GONE
    }

    override fun onItemClick(napBox: NapBoxResponse) {
//
//            val destination =
//                NapBoxesListFragmentDirections.actionNavSeeNapBoxesToNapBoxeDetailsFragment(
//                    napBox
//                )
//            findNavController().navigate(destination)
    }
}
