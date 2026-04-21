package cz.grimir.wifimanager.captive.web

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wifimanager.captive.login")
data class CaptiveLoginProperties(
    val accountNameFormat: String? = null,
) {
    val resolvedAccountNameFormat: String?
        get() = accountNameFormat?.trim()?.takeIf { it.isNotEmpty() }
}
