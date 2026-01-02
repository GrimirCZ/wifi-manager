package cz.grimir.wifimanager.admin.application.commands

import cz.grimir.wifimanager.shared.application.UserIdentitySnapshot
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
    val user: UserIdentitySnapshot,
)
