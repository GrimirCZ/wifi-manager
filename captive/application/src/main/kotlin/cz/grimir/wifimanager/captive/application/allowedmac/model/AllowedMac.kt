package cz.grimir.wifimanager.captive.application.allowedmac.model

import java.time.Instant

data class AllowedMac(
    val mac: String,
    val validUntil: Instant?,
)
