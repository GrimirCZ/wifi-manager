package cz.grimir.wifimanager.shared.events

import cz.grimir.wifimanager.shared.TicketId
import java.time.Instant

data class ClientKickedEvent(
    val ticketId: TicketId,
    val deviceMacAddress: String,
    val kickedAt: Instant,
)

