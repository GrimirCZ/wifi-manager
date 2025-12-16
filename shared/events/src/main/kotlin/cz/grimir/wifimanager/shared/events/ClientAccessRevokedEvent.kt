package cz.grimir.wifimanager.shared.events

import cz.grimir.wifimanager.shared.core.TicketId
import java.time.Instant

data class ClientAccessRevokedEvent(
    val ticketId: TicketId,
    val deviceMacAddress: String,
    val revokedAt: Instant,
)

