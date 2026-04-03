package cz.grimir.wifimanager.captive.application.migration.port

import cz.grimir.wifimanager.captive.application.migration.model.MigratableDirectoryUser

fun interface MigratableUserDirectoryPort {
    fun resolveByEmail(email: String): MigratableDirectoryUser?
}
