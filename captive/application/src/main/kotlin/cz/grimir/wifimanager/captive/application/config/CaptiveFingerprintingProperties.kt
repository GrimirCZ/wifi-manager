package cz.grimir.wifimanager.captive.application.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wifimanager.captive.fingerprinting")
data class CaptiveFingerprintingProperties(
    val enabled: Boolean = true,
    val logThreshold: Int = 40,
    val enforceThreshold: Int = 70,
    val trustedProxyEnabled: Boolean = false,
    val trustedTlsHeaderName: String = "X-Device-Tls-Fingerprint",
)