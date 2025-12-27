package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.shared.core.TicketId

data class CreateAuthorizedDevice(
    val mac: String,
    val name: String,
    val ticketId: TicketId,
    val wasKicked: Boolean,
)
