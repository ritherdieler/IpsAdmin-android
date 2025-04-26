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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.databinding.ActivityMainBinding
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.UpdateDeviceTokenUseCase
import com.dscorp.ispadmin.presentation.fcm.FcmTopics
import com.dscorp.ispadmin.presentation.fcm.updateFcmToken
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val firebaseAnalytics: FirebaseAnalytics by inject()
    private val viewModel: MainActivityViewModel by inject()
    private val updateDeviceTokenUseCase: UpdateDeviceTokenUseCase by inject()

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

        // Actualizar token FCM
        updateFcmToken()
    }

    private fun updateFcmToken() {
        val currentUser = viewModel.getCurrentUser()
        currentUser?.let { user ->
            lifecycleScope.launch {
                updateFcmToken(
                    context = this@MainActivity,
                    user = user,
                    updateDeviceTokenUseCase = updateDeviceTokenUseCase
                )
            }
        }
    }

    private fun setupNavigation() {
        navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = findNavController(R.id.nav_host_fragment_content_main)

        // Inicializar el grafo de navegación
        val navInflater = navHostFragment.navController.navInflater
        val graph = navInflater.inflate(R.navigation.main_nav_graph)
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
//            navigateToInitialDestination(user)
        }
    }

    private fun configureMenuBasedOnUserType(user: User) {
        val menu = binding.navView.menu


        // Configuración específica para cada tipo de usuario
        when (user.type) {
            User.UserType.TECHNICIAN -> {
                // Técnicos sólo pueden ver las órdenes cerrar/cancelar
                menu.findItem(R.id.assignedInstallationOrdersFragment).isVisible = true
                menu.findItem(R.id.nav_subscriptions_menu).isVisible = true
            }

            User.UserType.SECRETARY -> {
                menu.findItem(R.id.nav_dashboard).isVisible = true
                menu.findItem(R.id.pendingInstallationOrdersFragment).isVisible = true
                menu.findItem(R.id.nav_subscriptions_menu).isVisible = true

            }

            User.UserType.ACCOUNTANT -> {
                menu.findItem(R.id.nav_dashboard).isVisible = true
                menu.findItem(R.id.nav_outlays).isVisible = true
                menu.findItem(R.id.nav_fixed_cost).isVisible = true
                menu.findItem(R.id.pendingInstallationOrdersFragment).isVisible = true
                menu.findItem(R.id.nav_subscriptions_menu).isVisible = true
            }

            User.UserType.ADMIN -> {
                // Hacemos visible cualquier otra opción del menú que pueda estar oculta por defecto
                for (i in 0 until menu.size()) {
                    menu.getItem(i).isVisible = true
                }
            }

            User.UserType.SALES -> {
                // Los usuarios de ventas siempre deben ver la opción de crear órdenes de instalación
                menu.findItem(R.id.nav_create_installation_order).isVisible = true
                // Mostrar las opciones para ver órdenes en progreso y cerradas
                menu.findItem(R.id.sellerInProgressOrdersFragment).isVisible = true
                menu.findItem(R.id.sellerClosedOrdersFragment).isVisible = true
                // Aseguramos que el menú padre sea visible
                menu.findItem(R.id.nav_installation_orders).isVisible = true
            }

            else -> { /* No hacer cambios para otros tipos de usuario */
            }
        }

        // Configurar visibilidad para tickets de soporte
        configureSupportTicketVisibility(user)
    }

    private fun configureSupportTicketVisibility(user: User) {
        val menu = binding.navView.menu
        val showCreateTicket = user.type in listOf(
            User.UserType.ADMIN,
            User.UserType.SECRETARY,
            User.UserType.ACCOUNTANT
        )
        val showSupportTickets = user.type in listOf(
            User.UserType.ADMIN,
            User.UserType.SECRETARY,
            User.UserType.TECHNICIAN,
            User.UserType.ACCOUNTANT
        )

        menu.findItem(R.id.nav_create_support_ticket).isVisible = showCreateTicket
        menu.findItem(R.id.nav_support_assistance_tickets).isVisible = showSupportTickets
    }

    private fun configureInstallationOrdersForAdmin(menu: Menu) {
        // Los administradores y secretarias pueden ver todas las opciones
        menu.findItem(R.id.assignedInstallationOrdersFragment).isVisible = true
        menu.findItem(R.id.nav_create_installation_order).isVisible = true
        menu.findItem(R.id.pendingInstallationOrdersFragment).isVisible = true

    }

    private fun subscribeToFcmTopicsForUser(user: User) {
        val messaging = FirebaseMessaging.getInstance()

        // Suscripciones específicas basadas en el tipo de usuario
        when (user.type) {
            User.UserType.TECHNICIAN -> {
                messaging.subscribeToTopic(FcmTopics.FCM_TECHNICIAN_TOPIC)
                messaging.subscribeToTopic(FcmTopics.ASSISTANCE_TICKET)
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

            User.UserType.SALES -> {
                messaging.subscribeToTopic(FcmTopics.TOPIC_INSTALLATION_ORDER)
            }

            else -> { /* No hay suscripciones especiales para otros tipos */
            }
        }
    }

    private fun navigateToInitialDestination(user: User) {
        // Como dashboard y find_subscriptions están ocultos, usamos nav_my_profile como destino inicial por defecto
        val destination = when (user.type) {
            User.UserType.SECRETARY -> R.id.nav_dashboard
            User.UserType.ADMIN -> R.id.nav_dashboard
            User.UserType.TECHNICIAN -> R.id.assignedInstallationOrdersFragment
            User.UserType.ACCOUNTANT -> R.id.nav_dashboard
            User.UserType.SALES -> R.id.nav_create_installation_order
            else -> R.id.nav_my_profile
        }

        // Navegar al destino inicial
        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(navController.graph.startDestinationId, false)
            .build()

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
