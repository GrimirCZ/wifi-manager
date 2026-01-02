package cz.grimir.wifimanager.shared.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wifimanager.security")
data class UiSecurityProperties(
    /**
     * OAuth2 client registration id used for UI OIDC login pages.
     */
    val oidcRegistrationId: String = "admin",
    /**
     * Optional mapping from an OAuth2 client registration id to an authorities provider key.
     */
    val authoritiesProviderByRegistration: Map<String, String> = emptyMap(),
) {
    init {
        require(authoritiesProviderByRegistration.isNotEmpty()) {
            "At least one authorities provider must be configured in 'wifimanager.security.authorities-provider-by-registration'."
        }
    }
}
