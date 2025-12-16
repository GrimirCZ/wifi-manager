package cz.grimir.wifimanager.admin.web.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wifimanager.admin.security")
data class AdminSecurityProperties(
    val username: String = "admin",
    val password: String = "admin",
)

