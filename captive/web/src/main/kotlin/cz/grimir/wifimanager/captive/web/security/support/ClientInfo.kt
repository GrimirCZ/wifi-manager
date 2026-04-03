package cz.grimir.wifimanager.captive.web.security.support

import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintProfile

data class ClientInfo(
    val ipAddress: String,
    val macAddress: String,
    val hostname: String?,
    val dhcpVendorClass: String?,
    val dhcpPrlHash: String?,
    val dhcpHostname: String?,
    val fingerprintProfile: DeviceFingerprintProfile?,
)
