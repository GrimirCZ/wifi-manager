package cz.grimir.wifimanager.shared.application.captive

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wifimanager.captive.portal")
data class CaptivePortalApiProperties(
    val publicBaseUrl: String = "https://localhost",
)
