package cz.grimir.wifimanager.captive.application.allowed

import java.time.Instant

data class AllowedMac(
    val mac: String,
    val validUntil: Instant?,
)
