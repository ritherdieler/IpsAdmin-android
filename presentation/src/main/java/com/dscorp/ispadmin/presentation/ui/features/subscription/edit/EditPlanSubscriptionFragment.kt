package com.dscorp.ispadmin.presentation.ui.features.subscription.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dscorp.ispadmin.presentation.extension.analytics.AnalyticsConstants
import com.dscorp.ispadmin.presentation.extension.analytics.sendTouchButtonEvent
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.features.subscription.edit.compose.EditPlanSubscriptionScreen
import com.google.firebase.analytics.FirebaseAnalytics
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class EditPlanSubscriptionFragment : Fragment() {
//    private val args by navArgs<EditPlanSubscriptionFragmentArgs>()
    private val viewModel: EditSubscriptionViewModel by viewModel()
    private val firebaseAnalytics: FirebaseAnalytics by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MyTheme {
                    EditPlanContent()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        viewModel.getFormData(args.subscriptionId)
    }

    @Composable
    private fun EditPlanContent() {
        // Observamos el estado desde el ViewModel (StateFlow)
        val state by viewModel.uiState.collectAsState()
        MyTheme {
            EditPlanSubscriptionScreen(
                state = state,
                onPlanSelected = { plan -> viewModel.updateSelectedPlan(plan) },
                onEditClick = {
                    firebaseAnalytics.sendTouchButtonEvent(AnalyticsConstants.REGISTER_SUBSCRIPTION)
//                    viewModel.editSubscription(args.subscriptionId)
                },
                onSuccessDialogDismiss = {
                    viewModel.clearSuccess()
                    findNavController().navigateUp()
                },
                onErrorDismiss = {
                    viewModel.clearError()
                }
            )
        }
    }
}

