package cz.grimir.wifimanager.admin.application.commands

import cz.grimir.wifimanager.shared.UserId
import java.time.Duration

data class CreateTicketCommand(
    val accessCode: String,
    val duration: Duration,
    val userId: UserId
)
