package cz.grimir.wifimanager.app.routing

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wifimanager.captive.request-routing")
data class CaptiveRequestRoutingProperties(
    val ipv4Subnets: List<String> = emptyList(),
    val ipv6Subnets: List<String> = emptyList(),
)
