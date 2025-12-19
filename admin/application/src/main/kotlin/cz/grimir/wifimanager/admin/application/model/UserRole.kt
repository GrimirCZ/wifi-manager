package cz.grimir.wifimanager.admin.application.model

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
