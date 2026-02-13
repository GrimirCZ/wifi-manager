package cz.grimir.wifimanager.captive.web.security.support

data class ClientInfo(
    val ipAddress: String,
    val macAddress: String,
    val hostname: String?,
)
