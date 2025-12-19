package cz.grimir.wifimanager.admin.core.aggregates

import cz.grimir.wifimanager.admin.core.exceptions.CannotCancelInactiveTicket
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant
import kotlin.jvm.Throws

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
     * Whether the ticket was manually canceled by the user or automatically by the expiration job.
     */
    var wasCanceled: Boolean = false,
    /**
     * ID of the tickets' creator.
     */
    var authorId: UserId,
) {
    /**
     * Checks whether the ticket is currently active (not expired and not canceled).
     *
     * We consider a ticket active if the current time is before its validUntil time, and it has not been canceled.
     *
     * Ticket is not considered active even though it has not been yet canceled by the expiration job.
     */
    fun isActive(at: Instant = Instant.now()): Boolean = !wasCanceled && validUntil.isAfter(at)

    /**
     * Cancels the ticket if it is currently active.
     *
     * @throws CannotCancelInactiveTicket if the ticket is not active.
     */
    @Throws(CannotCancelInactiveTicket::class)
    fun cancel(userId: UserId) {
        if (!isActive()) {
            throw CannotCancelInactiveTicket(userId, id)
        }

        wasCanceled = true
    }

    override fun toString(): String {
        return "Ticket(id=$id, accessCode='$accessCode', createdAt=$createdAt, validUntil=$validUntil, wasCanceled=$wasCanceled, authorId=$authorId)"
    }
}
