package cz.grimir.wifimanager.web.captive.security.support

data class ClientInfo(
    val ipAddress: String,
    val macAddress: String,
    val hostname: String?,
)
