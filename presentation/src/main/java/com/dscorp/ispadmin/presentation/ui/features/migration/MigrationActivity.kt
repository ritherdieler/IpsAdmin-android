package com.dscorp.ispadmin.presentation.ui.features.migration

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dscorp.ispadmin.databinding.ActivityMigrationActivityBinding
import com.dscorp.ispadmin.presentation.ui.components.Loader
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SUBSCRIPTION_ID
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MigrationActivity : AppCompatActivity() {

    private val viewModel: MigrationViewModel by viewModel()

    private lateinit var binding: ActivityMigrationActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMigrationActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val subscriptionId = intent.getIntExtra(SUBSCRIPTION_ID, -1)

        viewModel.getMigrationFormData(subscriptionId)

        lifecycleScope.launch {

            viewModel.uiState.collect {
                when (it) {
                    MigrationUiState.Empty -> {}
                    is MigrationUiState.Error -> showErrorDialog(it, subscriptionId)
                    is MigrationUiState.FormDataReady -> showMigrationForm(it)
                    MigrationUiState.Loading -> showLoader()
                    is MigrationUiState.Success -> {
                        Toast.makeText(
                            this@MigrationActivity,
                            "Migración exitosa",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            }
        }

    }

    private fun showLoader() {
        binding.migrationComposeView.setContent {
            Loader()
        }
    }

    private fun showMigrationForm(
        it: MigrationUiState.FormDataReady,
    ) {
        binding.migrationComposeView.setContent {
            MigrationForm(
                onus = it.unconfirmedOnus,
                plans = it.plans,
                subscription=it.subscription,
                isRefreshingOnuList = it.isRefreshingOnuList,
                onRefreshOnus = {
                    viewModel.refreshOnusDebounced(it.subscription.id)
                },
                onMigrationRequest = { request ->
                    request.apply { subscriptionId = it.subscription.id }
                    viewModel.doMigration(request)
                }
            )
        }
    }

    private fun showErrorDialog(it: MigrationUiState.Error, subscriptionId: Int) {
        binding.migrationComposeView.setContent {
            ErrorDialog(
                error = it.error.message ?: "",
                onDismissRequest = {
                    viewModel.getMigrationFormData(subscriptionId)
                }
            )
        }
    }

}