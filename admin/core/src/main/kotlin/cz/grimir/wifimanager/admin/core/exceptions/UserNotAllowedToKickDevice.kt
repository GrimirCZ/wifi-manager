package cz.grimir.wifimanager.admin.core.exceptions

import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId

class UserNotAllowedToKickDevice(
    val userId: UserId,
    val ticketId: TicketId,
) : RuntimeException("User with ID $userId is not allowed to kick devices from ticket $ticketId")
