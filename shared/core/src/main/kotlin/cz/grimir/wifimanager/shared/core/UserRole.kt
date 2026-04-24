package cz.grimir.wifimanager.shared.core

enum class UserRole(
    val canHaveMultipleTickets: Boolean = false,
    val canCancelOtherUsersTickets: Boolean = false,
    val canManageAllowedMacs: Boolean = false,
    val canCreateExtendedTickets: Boolean = false,
) {
    WIFI_ADMIN(
        canHaveMultipleTickets = true,
        canCancelOtherUsersTickets = true,
        canManageAllowedMacs = true,
        canCreateExtendedTickets = true,
    ),
    WIFI_STAFF,
}
