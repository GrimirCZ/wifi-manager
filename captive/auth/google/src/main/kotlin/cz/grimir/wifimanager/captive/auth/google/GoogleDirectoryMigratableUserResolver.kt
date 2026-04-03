package cz.grimir.wifimanager.captive.auth.google

import cz.grimir.wifimanager.captive.application.migration.model.MigratableDirectoryUser
import cz.grimir.wifimanager.captive.application.migration.port.MigratableUserDirectoryPort

class GoogleDirectoryMigratableUserResolver(
    private val directoryApiClient: GoogleDirectoryApiClient,
    private val allowedDevicesByGroup: Map<String, Int>,
) : MigratableUserDirectoryPort {
    override fun resolveByEmail(email: String): MigratableDirectoryUser? =
        try {
            val user = directoryApiClient.fetchUser(email)
            val groups = directoryApiClient.fetchGroups(user.primaryEmail)
            MigratableDirectoryUser(
                subject = user.id,
                email = user.primaryEmail,
                displayName = user.fullName,
                pictureUrl = user.thumbnailPhotoUrl,
                groups = groups,
                allowedDeviceCount = groups.mapNotNull { allowedDevicesByGroup[it] }.maxOrNull() ?: 0,
            )
        } catch (_: Exception) {
            null
        }
}
