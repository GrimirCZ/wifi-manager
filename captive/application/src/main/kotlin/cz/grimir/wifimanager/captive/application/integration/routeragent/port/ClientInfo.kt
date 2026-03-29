package cz.grimir.wifimanager.captive.application.integration.routeragent.port

data class ClientInfo(
    val macAddress: String,
    val hostname: String?,
    val dhcpVendorClass: String? = null,
    val dhcpPrlHash: String? = null,
    val dhcpHostname: String? = null,
)
