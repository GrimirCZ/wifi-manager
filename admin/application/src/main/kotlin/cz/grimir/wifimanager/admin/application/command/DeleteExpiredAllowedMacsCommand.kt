package cz.grimir.wifimanager.admin.application.command

import java.time.Instant

data class DeleteExpiredAllowedMacsCommand(
    val at: Instant,
)
