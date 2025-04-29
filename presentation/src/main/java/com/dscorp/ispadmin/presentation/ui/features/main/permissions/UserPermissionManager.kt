package com.dscorp.ispadmin.presentation.ui.features.main.permissions

import androidx.annotation.IdRes
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.User.UserType

/**
 * Gestor de permisos que configura las funcionalidades disponibles 
 * para cada tipo de usuario de forma centralizada.
 */
class UserPermissionManager {
    
    /**
     * Obtiene la configuración de funcionalidades para un tipo de usuario específico.
     * @param userType el tipo de usuario
     * @return la configuración de funcionalidades correspondiente
     */
    fun getFeatureConfigForUser(userType: UserType): FeatureConfig {
        return when (userType) {
            UserType.ADMIN -> createAdminConfig()
            UserType.SECRETARY -> createSecretaryConfig()
            UserType.TECHNICIAN -> createTechnicianConfig()
            UserType.ACCOUNTANT -> createAccountantConfig()
            UserType.SALES -> createSalesConfig()
            else -> createDefaultConfig()
        }
    }
    
    /**
     * Obtiene el destino inicial de navegación para un tipo de usuario específico.
     * @param userType el tipo de usuario
     * @return el ID del recurso del destino de navegación, o null para usar el destino predeterminado
     */
    @IdRes
    fun getInitialDestination(userType: UserType): Int? {
        return when (userType) {
            UserType.SECRETARY -> R.id.nav_dashboard
            UserType.ADMIN -> R.id.nav_dashboard
            UserType.TECHNICIAN -> R.id.assignedInstallationOrdersFragment
            UserType.ACCOUNTANT -> R.id.nav_dashboard
            UserType.SALES -> R.id.nav_create_installation_order
            else -> null // Usará el destino predeterminado (nav_my_profile)
        }
    }
    
    private fun createAdminConfig(): FeatureConfig {
        return FeatureConfig(
            hasDashboardAccess = true,
            canCreateInstallationOrders = true,
            canViewSellerInProgressOrders = true,
            canViewSellerClosedOrders = true,
            canViewPendingInstallationOrders = true,
            canViewAssignedInstallationOrders = true,
            hasAnyInstallationOrderAccess = true,
            hasSubscriptionsAccess = true,
            canCreateSupportTickets = true,
            canViewSupportTickets = true,
            hasOutlaysAccess = true,
            hasFixedCostAccess = true,
            canDeleteOnu = true
        )
    }
    
    private fun createSecretaryConfig(): FeatureConfig {
        return FeatureConfig(
            hasDashboardAccess = true,
            canViewPendingInstallationOrders = true,
            hasAnyInstallationOrderAccess = true,
            hasSubscriptionsAccess = true,
            canCreateSupportTickets = true,
            canViewSupportTickets = true
        )
    }
    
    private fun createTechnicianConfig(): FeatureConfig {
        return FeatureConfig(
            canViewAssignedInstallationOrders = true,
            hasAnyInstallationOrderAccess = true,
            hasSubscriptionsAccess = true,
            canViewSupportTickets = true,
            canDeleteOnu = true
        )
    }
    
    private fun createAccountantConfig(): FeatureConfig {
        return FeatureConfig(
            hasDashboardAccess = true,
            canViewPendingInstallationOrders = true,
            hasAnyInstallationOrderAccess = true,
            hasSubscriptionsAccess = true,
            canCreateSupportTickets = true,
            canViewSupportTickets = true,
            hasOutlaysAccess = true,
            hasFixedCostAccess = true
        )
    }
    
    private fun createSalesConfig(): FeatureConfig {
        return FeatureConfig(
            canCreateInstallationOrders = true,
            canViewSellerInProgressOrders = true,
            canViewSellerClosedOrders = true,
            hasAnyInstallationOrderAccess = true
        )
    }
    
    private fun createDefaultConfig(): FeatureConfig {
        return FeatureConfig()
    }
} 