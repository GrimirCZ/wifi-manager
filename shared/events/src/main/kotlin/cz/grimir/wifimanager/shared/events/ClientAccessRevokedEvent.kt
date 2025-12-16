package cz.grimir.wifimanager.shared.events

import cz.grimir.wifimanager.shared.TicketId
import java.time.Instant

data class ClientAccessRevokedEvent(
    val ticketId: TicketId,
    val deviceMacAddress: String,
    val revokedAt: Instant,
)

