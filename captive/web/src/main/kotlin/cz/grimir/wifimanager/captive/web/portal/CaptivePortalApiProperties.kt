package cz.grimir.wifimanager.captive.web.portal

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wifimanager.captive.portal")
data class CaptivePortalApiProperties(
    val publicBaseUrl: String = "https://localhost",
)
