package cz.grimir.wifimanager.captive.application.migration.model

import java.time.Instant
import java.util.UUID

data class ImportMigratedUserCommand(
    val email: String,
    val tickets: List<ImportedActiveTicket>,
)

data class ImportedActiveTicket(
    val start: Instant,
    val lengthSeconds: Long,
    val authorizedDevices: List<ImportedAuthorizedDevice>,
)

data class ImportedAuthorizedDevice(
    val mac: String,
    val deviceName: String?,
    val displayName: String?,
)

data class MigrationImportResult(
    val userId: UUID,
    val identityId: UUID,
    val email: String,
    val importedTicketCount: Int,
    val importedDeviceCount: Int,
)
