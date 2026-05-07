package cz.grimir.wifimanager.captive.application.command

import java.time.Instant

data class AllowedClientsPresenceEntry(
    val macAddress: String,
    val lastSeenAt: Instant,
)

data class ApplyAllowedClientsPresenceCommand(
    val entries: List<AllowedClientsPresenceEntry>,
)
