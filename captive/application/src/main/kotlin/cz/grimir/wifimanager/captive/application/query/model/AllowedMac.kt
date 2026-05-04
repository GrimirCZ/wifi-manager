package cz.grimir.wifimanager.captive.application.query.model

import java.time.Instant

data class AllowedMac(
    val mac: String,
    val validUntil: Instant?,
)
