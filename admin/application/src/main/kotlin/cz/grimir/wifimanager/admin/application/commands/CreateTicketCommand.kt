package cz.grimir.wifimanager.admin.application.commands

import cz.grimir.wifimanager.admin.application.model.User
import cz.grimir.wifimanager.admin.application.model.UserIdentity
import cz.grimir.wifimanager.shared.core.UserId
import java.time.Duration

data class CreateTicketCommand(
    /**
     * Optional access code for the ticket. If null, a random access code will be generated.
     */
    val accessCode: String?,
    /**
     * Duration for which the ticket will be valid.
     */
    val duration: Duration,
    /**
     * User that request ticket creation.
     */
    val user: UserIdentity,
)
