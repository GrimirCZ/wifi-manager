package cz.grimir.wifimanager.captive.application.migration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wifimanager.captive.migration")
data class CaptiveMigrationProperties(
    val endpointPath: String = "/captive/api/migration/import-user",
)
