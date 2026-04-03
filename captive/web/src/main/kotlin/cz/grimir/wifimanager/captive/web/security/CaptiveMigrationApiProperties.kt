package cz.grimir.wifimanager.captive.web.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wifimanager.captive.migration.api")
data class CaptiveMigrationApiProperties(
    val headerName: String = "X-API-Key",
    val apiKey: String = "",
)
