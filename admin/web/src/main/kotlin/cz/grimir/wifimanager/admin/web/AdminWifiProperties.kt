package cz.grimir.wifimanager.admin.web

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wifimanager.wifi")
data class AdminWifiProperties(
    /**
     * SSID displayed in admin ticket instructions.
     */
    val ssid: String = "WiFi",
)
