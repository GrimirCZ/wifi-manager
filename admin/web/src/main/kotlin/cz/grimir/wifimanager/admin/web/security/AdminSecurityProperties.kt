package cz.grimir.wifimanager.admin.web.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wifimanager.admin.security")
data class AdminSecurityProperties(
    /**
     * Spring Security OAuth2 client registration id used for admin OIDC login.
     *
     * The actual provider configuration is expected to be supplied by the shell app later.
     */
    val oidcRegistrationId: String = "admin",
)
