package com.dscorp.ispadmin.presentation.ui.features.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dscorp.ispadmin.presentation.theme.MyTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class DashBoardFragment : Fragment() {
    
    private val viewModel: DashBoardViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MyTheme {
                    // Observamos el estado desde el ViewModel usando collectAsState
                    val uiState by viewModel.state.collectAsState()
                    
                    // Utilizamos nuestro componente Compose mejorado
                    DashboardScreen(
                        uiState = uiState,
                        onRefresh = {
                            // Cuando se solicita un refresco, llamamos al método del ViewModel
                            viewModel.getDashBoardData()
                        }
                    )
                }
            }
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Observamos eventos del ViewModel para manejar errores u otros eventos
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collectLatest { state ->
                    state.event?.let { event ->
                        when (event) {
                            is DashboardEvent.ShowError -> {
                                // Aquí podríamos mostrar un mensaje de error si fuera necesario
                                // Por ejemplo, usando un Snackbar o un Toast
                            }
                            else -> { /* Ignorar otros eventos */ }
                        }
                        // Informamos al ViewModel que el evento ha sido manejado
                        viewModel.onEventHandled()
                    }
                }
            }
        }
    }
}
