package com.dscorp.ispadmin.presentation.ui.features.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import com.dscorp.ispadmin.presentation.extension.showCurrentSimpleName
import com.dscorp.ispadmin.presentation.extension.showErrorDialog

abstract class BaseActivity<T, U : ViewDataBinding> : AppCompatActivity() {

    protected abstract val viewModel: BaseViewModel<T>
    protected abstract val binding: U
    protected abstract fun handleState(state: T)



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        viewModel.uiState.observe(this) { uiState ->
            uiState.error?.let { error -> showErrorDialog(error.message ?: "") }
            uiState.loading?.let { isLoading -> onLoading(isLoading) }
            uiState.uiState?.let { handleState(it) }
        }
    }

    protected open fun onLoading(isLoading: Boolean) {

    }

    protected open fun onViewReady(savedInstanceState: Bundle?) {}


    override fun onResume() {
        super.onResume()
        showCurrentSimpleName()
    }
}
