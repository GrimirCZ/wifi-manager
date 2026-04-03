package cz.grimir.wifimanager.admin.application.migration.command

import cz.grimir.wifimanager.shared.application.identity.model.UserIdentitySnapshot
import java.time.Instant

data class ImportAdminMigratedTicketsCommand(
    val user: UserIdentitySnapshot,
    val tickets: List<ImportedAdminTicket>,
) {
    data class ImportedAdminTicket(
        val start: Instant,
        val lengthSeconds: Long,
        val requireUserNameOnLogin: Boolean = false,
        val authorizedDevices: List<ImportedAdminAuthorizedDevice>,
    )

    data class ImportedAdminAuthorizedDevice(
        val mac: String,
        val displayName: String?,
        val deviceName: String?,
    )
}
