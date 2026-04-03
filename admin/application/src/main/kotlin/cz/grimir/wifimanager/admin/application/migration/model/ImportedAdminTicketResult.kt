package cz.grimir.wifimanager.admin.application.migration.model

import cz.grimir.wifimanager.shared.core.TicketId
import java.time.Instant

data class ImportedAdminTicketResult(
    val ticketId: TicketId,
    val accessCode: String,
    val createdAt: Instant,
    val validUntil: Instant,
)
