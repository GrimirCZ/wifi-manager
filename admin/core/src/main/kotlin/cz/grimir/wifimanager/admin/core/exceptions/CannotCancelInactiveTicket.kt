package cz.grimir.wifimanager.admin.core.exceptions

import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId

/**
 * User is trying to cancel a ticket that is not active.
 */
class CannotCancelInactiveTicket(
    val userId: UserId,
    val ticketId: TicketId,
) : Exception("User $userId is trying to cancel inactive ticket $ticketId")
