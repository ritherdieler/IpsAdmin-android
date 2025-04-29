package com.dscorp.ispadmin.presentation.ui.features.main.permissions

/**
 * Clase que representa la configuración de funcionalidades disponibles para un tipo de usuario.
 * Cada propiedad representa un permiso o acceso a una característica específica de la aplicación.
 */
data class FeatureConfig(
    // Dashboard
    val hasDashboardAccess: Boolean = false,
    
    // Installation orders
    val canCreateInstallationOrders: Boolean = false,
    val canViewSellerInProgressOrders: Boolean = false,
    val canViewSellerClosedOrders: Boolean = false,
    val canViewPendingInstallationOrders: Boolean = false,
    val canViewAssignedInstallationOrders: Boolean = false,
    val hasAnyInstallationOrderAccess: Boolean = false,
    
    // Subscriptions
    val hasSubscriptionsAccess: Boolean = false,
    
    // Support tickets
    val canCreateSupportTickets: Boolean = false,
    val canViewSupportTickets: Boolean = false,
    
    // Financials
    val hasOutlaysAccess: Boolean = false,
    val hasFixedCostAccess: Boolean = false,
    
    // Technical
    val canDeleteOnu: Boolean = false
) 