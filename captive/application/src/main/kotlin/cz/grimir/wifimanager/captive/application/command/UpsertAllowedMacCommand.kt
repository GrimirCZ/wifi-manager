package cz.grimir.wifimanager.captive.application.command

import java.time.Instant

data class UpsertAllowedMacCommand(
    val macAddress: String,
    val validUntil: Instant?,
)
