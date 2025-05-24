package com.dscorp.ispadmin.presentation.ui.features.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.dscorp.ispadmin.presentation.extension.analytics.sendScreen
import com.dscorp.ispadmin.presentation.extension.showCurrentSimpleName
import com.dscorp.ispadmin.presentation.extension.showErrorDialog
import com.google.firebase.analytics.FirebaseAnalytics
import org.koin.android.ext.android.inject

abstract class BaseFragment<T, U : ViewDataBinding> : Fragment() {
    protected val firebaseAnalytics: FirebaseAnalytics by inject()
    protected abstract val viewModel: BaseViewModel<T>
    protected abstract val binding: U

    protected abstract fun handleState(state: T)



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        onViewReady(savedInstanceState)
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            uiState.error?.let { error -> showErrorDialog(error.message ?: "") }
            uiState.uiState?.let { handleState(it) }
        }
        return binding.root
    }

    protected open fun onLoading(isLoading: Boolean) {

    }

    protected open fun onViewReady(savedInstanceState: Bundle?) {}


    override fun onResume() {
        super.onResume()
        showCurrentSimpleName()
        firebaseAnalytics.sendScreen(this::class.java.simpleName)
    }
}
