package cz.grimir.wifimanager.shared.events

import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant

data class TicketCreatedEvent(
    val id: TicketId,
    /**
     * Access code required to use this ticket.
     */
    val accessCode: String,
    /**
     * UTC time of ticket creation.
     */
    val createdAt: Instant,
    /**
     * UTC time of ticket expiration.
     */
    var validUntil: Instant,
    val author: Author,
) {
    data class Author(
        val userId: UserId,
        val email: String,
        val displayName: String,
    )
}
