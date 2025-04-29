package com.dscorp.ispadmin.presentation.ui.features.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.databinding.ActivityMainBinding
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.UpdateDeviceTokenUseCase
import com.dscorp.ispadmin.presentation.fcm.FcmTopics
import com.dscorp.ispadmin.presentation.fcm.updateFcmToken
import com.dscorp.ispadmin.presentation.ui.features.main.permissions.FcmTopicManager
import com.dscorp.ispadmin.presentation.ui.features.main.permissions.FeatureConfig
import com.dscorp.ispadmin.presentation.ui.features.main.permissions.UserPermissionManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val firebaseAnalytics: FirebaseAnalytics by inject()
    private val viewModel: MainActivityViewModel by inject()
    private val updateDeviceTokenUseCase: UpdateDeviceTokenUseCase by inject()
    private lateinit var userPermissionManager: UserPermissionManager
    private lateinit var fcmTopicManager: FcmTopicManager

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
        
        // Inicializar gestores de permisos
        userPermissionManager = UserPermissionManager()
        fcmTopicManager = FcmTopicManager(FirebaseMessaging.getInstance())
        
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
            
            // Aplicar configuración de permisos y suscripción a tópicos FCM
            applyUserConfiguration(user)
            
            // Aquí podríamos volver a habilitar la navegación inicial si se desea
            // navigateToInitialDestination(user)
        }
    }
    
    private fun applyUserConfiguration(user: User) {
        // Aplicar configuración de menú según permisos
        val featureConfig = userPermissionManager.getFeatureConfigForUser(user.type!!)
        configureMenuWithFeatures(featureConfig)
        
        // Configurar suscripciones FCM
        fcmTopicManager.subscribeToTopicsForUserType(user.type!!)
    }
    
    private fun configureMenuWithFeatures(featureConfig: FeatureConfig) {
        val menu = binding.navView.menu
        
        // Dashboard
        menu.findItem(R.id.nav_dashboard).isVisible = featureConfig.hasDashboardAccess
        
        // Installation orders
        menu.findItem(R.id.nav_create_installation_order).isVisible = featureConfig.canCreateInstallationOrders
        menu.findItem(R.id.sellerInProgressOrdersFragment).isVisible = featureConfig.canViewSellerInProgressOrders
        menu.findItem(R.id.sellerClosedOrdersFragment).isVisible = featureConfig.canViewSellerClosedOrders
        menu.findItem(R.id.pendingInstallationOrdersFragment).isVisible = featureConfig.canViewPendingInstallationOrders
        menu.findItem(R.id.assignedInstallationOrdersFragment).isVisible = featureConfig.canViewAssignedInstallationOrders
        menu.findItem(R.id.nav_installation_orders).isVisible = featureConfig.hasAnyInstallationOrderAccess
        
        // Subscriptions
        menu.findItem(R.id.nav_subscriptions_menu).isVisible = featureConfig.hasSubscriptionsAccess
        
        // Support tickets
        menu.findItem(R.id.nav_create_support_ticket).isVisible = featureConfig.canCreateSupportTickets
        menu.findItem(R.id.nav_support_assistance_tickets).isVisible = featureConfig.canViewSupportTickets
        
        // Financials
        menu.findItem(R.id.nav_outlays).isVisible = featureConfig.hasOutlaysAccess
        menu.findItem(R.id.nav_fixed_cost).isVisible = featureConfig.hasFixedCostAccess
        
        // Technical
        menu.findItem(R.id.nav_deleteOnuFragment).isVisible = featureConfig.canDeleteOnu
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

