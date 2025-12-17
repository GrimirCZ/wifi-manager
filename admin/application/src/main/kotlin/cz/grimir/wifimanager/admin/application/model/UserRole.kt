package cz.grimir.wifimanager.admin.application.model

enum class UserRole(
    val canHaveMultipleTickets: Boolean = false,
) {
    WIFI_ADMIN(canHaveMultipleTickets = true),
    WIFI_STAFF,
}
