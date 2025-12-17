package cz.grimir.wifimanager.admin.core.exceptions

import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId

/**
 * User is not an admin and already has an active ticket.
 */
class UserAlreadyHasActiveTickets(
    val userId: UserId,
    val openTicketIds: List<TicketId>,
) : RuntimeException("User with ID $userId already has active tickets: $openTicketIds")
