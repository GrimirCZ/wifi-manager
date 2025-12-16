package cz.grimir.wifimanager.captive.application.command

import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant

data class CreateCodeBasedAuthorizationCommand(
    val ticketId: TicketId,

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

    val userId: UserId,
)
