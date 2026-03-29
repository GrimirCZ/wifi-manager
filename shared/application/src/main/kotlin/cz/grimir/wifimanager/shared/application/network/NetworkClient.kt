package cz.grimir.wifimanager.shared.application.network

data class NetworkClient(
    val macAddress: String,
    val ipAddresses: List<String>,
    val hostname: String?,
    val allowed: Boolean,
    val dhcpVendorClass: String? = null,
    val dhcpPrlHash: String? = null,
    val dhcpHostname: String? = null,
)
