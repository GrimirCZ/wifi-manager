package cz.grimir.wifimanager.captive.application.migration.model

data class MigratableDirectoryUser(
    val subject: String,
    val email: String,
    val displayName: String,
    val pictureUrl: String?,
    val groups: Set<String>,
    val allowedDeviceCount: Int,
)
