package cz.grimir.wifimanager.shared.events

import cz.grimir.wifimanager.shared.core.TicketId
import java.time.Instant

data class AuthorizationTokenRemovedEvent(
    val ticketId: TicketId,
    val removedAt: Instant,
)
