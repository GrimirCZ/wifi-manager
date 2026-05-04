package cz.grimir.wifimanager.captive.application.port

data class ClientInfo(
    val macAddress: String,
    val hostname: String?,
    val dhcpVendorClass: String? = null,
    val dhcpPrlHash: String? = null,
    val dhcpHostname: String? = null,
)
