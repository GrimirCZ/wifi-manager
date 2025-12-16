package cz.grimir.wifimanager.shared.events

import cz.grimir.wifimanager.shared.core.TicketId
import java.time.Instant

data class TicketEndedEvent(
    val ticketId: TicketId,
    val endedAt: Instant,
    val reason: Reason,
) {
    enum class Reason {
        MANUAL,
        EXPIRED,
    }
}
