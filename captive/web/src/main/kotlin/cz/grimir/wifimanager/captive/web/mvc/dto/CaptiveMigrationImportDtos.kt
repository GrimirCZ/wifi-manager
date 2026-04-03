package cz.grimir.wifimanager.captive.web.mvc.dto

import java.time.Instant
import java.util.UUID

data class CaptiveMigrationImportRequest(
    val email: String,
    val tickets: List<CaptiveMigrationImportedTicketRequest>,
)

data class CaptiveMigrationImportedTicketRequest(
    val start: Instant?,
    val lengthSeconds: Long,
    val authorizedDevices: List<CaptiveMigrationImportedDeviceRequest>,
)

data class CaptiveMigrationImportedDeviceRequest(
    val mac: String,
    val deviceName: String?,
    val displayName: String?,
)

data class CaptiveMigrationImportResponse(
    val userId: UUID,
    val identityId: UUID,
    val email: String,
    val importedTicketCount: Int,
    val importedDeviceCount: Int,
)
