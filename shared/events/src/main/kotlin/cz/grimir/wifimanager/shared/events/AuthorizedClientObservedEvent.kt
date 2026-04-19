package cz.grimir.wifimanager.shared.events

data class AuthorizedClientObservedEvent(
    val macAddress: String,
    val hostname: String?,
    val dhcpHostname: String?,
    val dhcpVendorClass: String?,
    val dhcpPrlHash: String?,
)
