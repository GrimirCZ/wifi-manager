package cz.grimir.wifimanager.shared.security.oidc

import cz.grimir.wifimanager.shared.security.UiSecurityProperties
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component

@Component
class OidcAuthoritiesProviderRegistry(
    private val providers: List<OidcAuthoritiesProvider>,
    private val securityProperties: UiSecurityProperties,
) {
    fun getAuthorities(
        oidcUser: OidcUser,
        userRequest: OidcUserRequest,
    ): Set<GrantedAuthority> {
        val registrationId = userRequest.clientRegistration.registrationId

        val configuredProviderKey =
            securityProperties.authoritiesProviderByRegistration[registrationId]
                ?: error("No authorities provider configured for registrationId='$registrationId'.")

        val provider =
            providers.firstOrNull { it.key.equals(configuredProviderKey, ignoreCase = true) }
                ?: error(
                    "No OidcAuthoritiesProvider found for key '$configuredProviderKey' (registrationId='$registrationId').",
                )

        return provider.getAuthorities(oidcUser, userRequest)
    }
}
