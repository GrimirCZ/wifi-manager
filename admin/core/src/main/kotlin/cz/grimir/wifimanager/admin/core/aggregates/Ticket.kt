package cz.grimir.wifimanager.admin.core.aggregates

import cz.grimir.wifimanager.shared.TicketId
import cz.grimir.wifimanager.shared.UserId
import java.time.Instant

class Ticket(
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

    /**
     * Whether the ticket was manually canceled by the user.
     */
    var wasCanceled: Boolean = false,

    /**
     * ID of the tickets' creator.
     */
    var authorId: UserId,
)