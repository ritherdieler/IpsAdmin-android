package com.dscorp.ispadmin.presentation.ui.features.report

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dscorp.ispadmin.databinding.FragmentReportsBinding
import com.dscorp.ispadmin.presentation.extension.getDownloadedFileUri
import com.dscorp.ispadmin.presentation.extension.openWithXlsxApp
import com.dscorp.ispadmin.presentation.ui.features.base.BaseFragment
import com.dscorp.ispadmin.presentation.ui.features.report.DownloadDocumentType.*
import com.dscorp.ispadmin.domain.model.DownloadDocumentResponse
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReportsFragment : BaseFragment<ReportsUiState, FragmentReportsBinding>() {

    override val binding by lazy { FragmentReportsBinding.inflate(layoutInflater) }

    override val viewModel: ReportsViewModel by viewModel()

    override fun handleState(state: ReportsUiState) {
        if (state is ReportsUiState.DocumentReady) {
            downloadFile(state.document)
        }
    }

    private fun downloadDocument(it: DownloadDocumentType) =
        when (it) {
            WITH_PAYMENT_COMMITMENT -> viewModel.downloadPaymentCommitmentSubscriptionsReport()
            SUSPENDED -> viewModel.downloadSuspendedSubscriptionsReport()
            CUT_OFF -> viewModel.downloadCutOffSubscriptionsReport()
            DEBTORS_FROM_PAST_MONTH -> viewModel.downloadPastMonthDebtorsReport()
            DEBTORS_CUT_OFF_CANDIDATES -> viewModel.downloadDebtorsCutOffCandidatesSubscriptionsReport()
            DEBTORS_WITH_ACTIVE_SERVICE -> viewModel.downloadDebtorWithActiveSubscriptionsReport()
            DEBTORS_WITH_CANCELLED_SERVICE -> viewModel.downloadDebtorWithCancelledSubscriptionsReport()
            CANCELLED_SUBSCRIPTIONS_FROM_CURRENT_MONTH -> viewModel.downloadCancelledSubscriptionsFromCurrentMonthReport()
            CANCELLED_SUBSCRIPTIONS_FROM_PAST_MONTH -> viewModel.downloadCancelledSubscriptionsFromPastMonthReport()
        }

    override fun onViewReady(savedInstanceState: Bundle?) {
        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        binding.apply {
            btnGetDebtorsCutOffCandidates.setOnClickListener {
                downloadDocument(DEBTORS_CUT_OFF_CANDIDATES)
            }
            btnGetDebtorsWithActiveServiceCustomers.setOnClickListener {
                downloadDocument(DEBTORS_WITH_ACTIVE_SERVICE)
            }
            btnGetDebtorsWithCancelledServiceCustomers.setOnClickListener {
                downloadDocument(DEBTORS_WITH_CANCELLED_SERVICE)
            }
            btnGetWithPaymentCommitmentCustomers.setOnClickListener {
                downloadDocument(WITH_PAYMENT_COMMITMENT)
            }
            btnSuspendedCustomers.setOnClickListener {
                downloadDocument(SUSPENDED)
            }
            btnGetCutoffCustomers.setOnClickListener {
                downloadDocument(CUT_OFF)
            }
            btnGetPastMonthDebtorCustomers.setOnClickListener {
                downloadDocument(DEBTORS_FROM_PAST_MONTH)
            }
            btnGetCancelledSubscriptionsCurrentMonth.setOnClickListener {
                downloadDocument(CANCELLED_SUBSCRIPTIONS_FROM_CURRENT_MONTH)
            }
            btnGetCancelledSubscriptionsFromPastMonth.setOnClickListener {
                downloadDocument(CANCELLED_SUBSCRIPTIONS_FROM_PAST_MONTH)
            }
        }
    }

    private fun downloadFile(document: DownloadDocumentResponse) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            openDocument(document)
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    12
                )
            } else {
                openDocument(document)
            }
        }
    }

    private fun openDocument(document: DownloadDocumentResponse) {
        val uri = requireActivity().getDownloadedFileUri(document)
        openWithXlsxApp(uri)
    }
}


