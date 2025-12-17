package cz.grimir.wifimanager.admin.web.security

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component

@Component
class AdminAuthoritiesProviderRegistry(
    private val providers: List<AdminAuthoritiesProvider>,
    private val securityProperties: AdminSecurityProperties,
) {
    fun getAuthorities(
        oidcUser: OidcUser,
        userRequest: OidcUserRequest,
    ): Set<GrantedAuthority> {
        val registrationId = userRequest.clientRegistration.registrationId

        val configuredProviderKey = securityProperties.authoritiesProviderByRegistration[registrationId]
            ?: error("No authorities provider configured for registrationId='$registrationId'.")

        val provider = providers.firstOrNull { it.key.equals(configuredProviderKey, ignoreCase = true) }
            ?: error(
                "No AdminAuthoritiesProvider found for key '$configuredProviderKey' (registrationId='$registrationId')."
            )

        return provider.getAuthorities(oidcUser, userRequest)
    }
}
