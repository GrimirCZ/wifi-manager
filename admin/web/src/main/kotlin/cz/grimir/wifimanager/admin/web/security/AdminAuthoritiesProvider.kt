package cz.grimir.wifimanager.admin.web.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.core.oidc.user.OidcUser

/**
 * Provides Spring Security authorities derived from an IdP login.
 *
 * Implementations are provider-specific (Keycloak, Google, ...).
 */
interface AdminAuthoritiesProvider {
    /**
     * Stable identifier used for configuration-based provider selection.
     */
    val key: String

    fun getAuthorities(
        oidcUser: OidcUser,
        userRequest: OidcUserRequest,
    ): Set<GrantedAuthority>
}
