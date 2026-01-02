package cz.grimir.wifimanager.shared.core

enum class UserRole(
    val canHaveMultipleTickets: Boolean = false,
    val canCancelOtherUsersTickets: Boolean = false,
) {
    WIFI_ADMIN(
        canHaveMultipleTickets = true,
        canCancelOtherUsersTickets = true,
    ),
    WIFI_STAFF,
}
