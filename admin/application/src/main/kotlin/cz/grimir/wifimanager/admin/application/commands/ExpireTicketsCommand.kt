package cz.grimir.wifimanager.admin.application.commands

import java.time.Instant

data class ExpireTicketsCommand(
    /**
     * Tickets which validUntil is before, or equal, this time will be expired.
     */
    val at: Instant = Instant.now(),
)