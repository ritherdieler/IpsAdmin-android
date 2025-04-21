package com.dscorp.ispadmin.presentation.ui.features.mufas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.dscorp.ispadmin.databinding.DialogNapboxDetailBinding
import com.dscorp.ispadmin.domain.model.NapBoxResponse

class NapBoxDetailDialogFragment(
    private val napBox: NapBoxResponse? = null,
    private val showSelectButton: Boolean = false,
    private val listener: NapBoxSelectionListener? = null
) : DialogFragment() {
    val binding by lazy { DialogNapboxDetailBinding.inflate(layoutInflater) }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding.napBox = napBox
        binding.executePendingBindings()
        binding.btnSelect.visibility = if (showSelectButton) View.VISIBLE else View.GONE

        binding.ivButtonClose.setOnClickListener {
            dismiss()
        }
        binding.btnSelect.setOnClickListener {
            listener?.onNapBoxSelected(napBox!!)
            dismiss()
        }

        return binding.root
    }

    interface NapBoxSelectionListener {
        fun onNapBoxSelected(napBox: NapBoxResponse)
    }


}