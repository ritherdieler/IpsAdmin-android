package com.dscorp.ispadmin.presentation.ui.features.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.databinding.ActivityMainBinding
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.fcm.FcmTopics
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val firebaseAnalytics: FirebaseAnalytics by inject()
    private val viewModel: MainActivityViewModel by inject()
    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment

    companion object {
        const val PERMISSION_CODE = 123
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        
        setupNavigation()
        checkNotificationPermission()
        setupUserProfile()
    }

    private fun setupNavigation() {
        navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = findNavController(R.id.nav_host_fragment_content_main)
        
        // Inicializar el grafo de navegación
        val navInflater = navHostFragment.navController.navInflater
        val graph = navInflater.inflate(R.navigation.mobile_navigation)
        navHostFragment.navController.graph = graph
        
        // Configurar el NavigationView
        binding.navView.setupWithNavController(navController)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                PERMISSION_CODE
            )
        }
    }

    private fun setupUserProfile() {
        // Suscribirse al tema común para todos los usuarios
        FirebaseMessaging.getInstance().subscribeToTopic(FcmTopics.FCM_ALL_TOPIC)
        
        viewModel.user?.let { user ->
            firebaseAnalytics.setUserId(user.id.toString())
            configureMenuBasedOnUserType(user)
            subscribeToFcmTopicsForUser(user)
            navigateToInitialDestination(user)
        }
    }

    private fun configureMenuBasedOnUserType(user: User) {
        val menu = binding.navView.menu
        
        // Configuración común
        when (user.type) {
            User.UserType.TECHNICIAN -> {
                menu.findItem(R.id.nav_dashboard).isVisible = false
                menu.findItem(R.id.nav_reports).isVisible = false
                menu.findItem(R.id.nav_see_plan_list).isVisible = false
                menu.findItem(R.id.nav_mufa).isVisible = true
                // Técnicos sólo pueden ver las órdenes cerrar/cancelar
                configureInstallationOrdersForTechnician(menu)
            }
            User.UserType.SECRETARY -> {
                menu.findItem(R.id.nav_dashboard).isVisible = true
                menu.findItem(R.id.nav_see_plan_list).isVisible = false
                menu.findItem(R.id.nav_mufa).isVisible = false
                // Secretarias pueden ver todas las opciones de órdenes
                configureInstallationOrdersForAdmin(menu)
            }
            User.UserType.ACCOUNTANT -> {
                menu.findItem(R.id.nav_dashboard).isVisible = true
                menu.findItem(R.id.nav_outlays).isVisible = true
                menu.findItem(R.id.nav_fixed_cost).isVisible = true
                menu.findItem(R.id.nav_see_plan_list).isVisible = false
                menu.findItem(R.id.nav_mufa).isVisible = false
                // Contadores pueden ver/crear pero no asignar técnicos
                configureInstallationOrdersForAccountant(menu)
            }
            User.UserType.ADMIN -> {
                menu.findItem(R.id.nav_outlays).isVisible = true
                menu.findItem(R.id.nav_fixed_cost).isVisible = true
                // Admins pueden ver todas las opciones de órdenes
                configureInstallationOrdersForAdmin(menu)
            }
            User.UserType.SALES -> {
                // Vendedores sólo pueden crear órdenes
                configureInstallationOrdersForSales(menu)
            }
            else -> { /* No hacer cambios para otros tipos de usuario */ }
        }
        
        // Configurar visibilidad para tickets de soporte
        configureSupportTicketVisibility(user)
    }

    private fun configureSupportTicketVisibility(user: User) {
        val menu = binding.navView.menu
        val showCreateTicket = user.type in listOf(User.UserType.ADMIN, User.UserType.SECRETARY)
        val showSupportTickets = user.type in listOf(User.UserType.ADMIN, User.UserType.SECRETARY, User.UserType.TECHNICIAN)
        
        menu.findItem(R.id.nav_create_support_ticket).isVisible = showCreateTicket
        menu.findItem(R.id.nav_support_assistance_tickets).isVisible = showSupportTickets
    }

    private fun configureInstallationOrdersForAdmin(menu: Menu) {
        // Los administradores y secretarias pueden ver todas las opciones
        menu.findItem(R.id.nav_installation_orders).isVisible = true
        menu.findItem(R.id.nav_create_installation_order).isVisible = true
        menu.findItem(R.id.pendingInstallationOrdersFragment).isVisible = true
        menu.findItem(R.id.nav_assign_technician).isVisible = true
        menu.findItem(R.id.nav_close_installation_order).isVisible = true
        menu.findItem(R.id.nav_cancel_installation_order).isVisible = true
    }
    
    private fun configureInstallationOrdersForTechnician(menu: Menu) {
        // Los técnicos sólo pueden cerrar/cancelar órdenes
        menu.findItem(R.id.nav_installation_orders).isVisible = true
        menu.findItem(R.id.nav_create_installation_order).isVisible = false
        menu.findItem(R.id.pendingInstallationOrdersFragment).isVisible = true
        menu.findItem(R.id.nav_assign_technician).isVisible = false
        menu.findItem(R.id.nav_close_installation_order).isVisible = true
        menu.findItem(R.id.nav_cancel_installation_order).isVisible = true
    }
    
    private fun configureInstallationOrdersForAccountant(menu: Menu) {
        // Los contadores pueden crear y ver, pero no pueden asignar técnicos
        menu.findItem(R.id.nav_installation_orders).isVisible = true
        menu.findItem(R.id.nav_create_installation_order).isVisible = true
        menu.findItem(R.id.pendingInstallationOrdersFragment).isVisible = true
        menu.findItem(R.id.nav_assign_technician).isVisible = false
        menu.findItem(R.id.nav_close_installation_order).isVisible = true
        menu.findItem(R.id.nav_cancel_installation_order).isVisible = true
    }
    
    private fun configureInstallationOrdersForSales(menu: Menu) {
        // Los vendedores sólo pueden crear órdenes
        menu.findItem(R.id.nav_installation_orders).isVisible = true
        menu.findItem(R.id.nav_create_installation_order).isVisible = true
        menu.findItem(R.id.pendingInstallationOrdersFragment).isVisible = true
        menu.findItem(R.id.nav_assign_technician).isVisible = false
        menu.findItem(R.id.nav_close_installation_order).isVisible = false
        menu.findItem(R.id.nav_cancel_installation_order).isVisible = false
    }

    private fun subscribeToFcmTopicsForUser(user: User) {
        val messaging = FirebaseMessaging.getInstance()
        
        // Suscripciones específicas basadas en el tipo de usuario
        when (user.type) {
            User.UserType.TECHNICIAN -> {
                messaging.subscribeToTopic(FcmTopics.FCM_TECHNICIAN_TOPIC)
                messaging.subscribeToTopic(FcmTopics.ASSISTANCE_TICKET)
                messaging.subscribeToTopic(FcmTopics.TOPIC_INSTALLATION_ORDER)
            }
            User.UserType.SECRETARY, User.UserType.ACCOUNTANT -> {
                messaging.subscribeToTopic(FcmTopics.FCM_SECRETARY_TOPIC)
                messaging.subscribeToTopic(FcmTopics.ASSISTANCE_TICKET_ADMINS)
                messaging.subscribeToTopic(FcmTopics.ASSISTANCE_TICKET)
                messaging.subscribeToTopic(FcmTopics.TOPIC_INSTALLATION_ORDER)

            }
            User.UserType.ADMIN -> {
                messaging.subscribeToTopic(FcmTopics.ASSISTANCE_TICKET_ADMINS)
                messaging.subscribeToTopic(FcmTopics.ASSISTANCE_TICKET)
                messaging.subscribeToTopic(FcmTopics.TOPIC_INSTALLATION_ORDER)
            }
            else -> { /* No hay suscripciones especiales para otros tipos */ }
        }
    }

    private fun navigateToInitialDestination(user: User) {
        val destination = when (user.type) {
            User.UserType.TECHNICIAN -> R.id.nav_register_subscription
            User.UserType.SECRETARY -> R.id.nav_find_subscriptions
            User.UserType.ADMIN -> R.id.nav_dashboard
            else -> R.id.nav_find_subscriptions
        }
        
        // Crear opciones de navegación para reemplazar la pila de navegación
        val navOptions = NavOptions.Builder()
            .setPopUpTo(navController.graph.startDestinationId, true)
            .build()
            
        // Navegar al destino inicial
        navController.navigate(destination, null, navOptions)
    }

    override fun onBackPressed() {
        if (navController.currentDestination?.id == R.id.nav_dashboard) {
            showExitConfirmationDialog()
        } else {
            super.onBackPressed()
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("Salir")
            setMessage("¿Estás seguro que quieres salir?")
            setPositiveButton("Sí") { _, _ -> finish() }
            setNegativeButton("No", null)
            create().show()
        }
    }
}
