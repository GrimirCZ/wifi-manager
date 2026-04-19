package cz.grimir.wifimanager.shared.security.oidc

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component

/**
 * Leaves provider-supplied authorities unchanged.
 *
 * Useful for OIDC providers where the app resolves application roles after login
 * and does not need provider-specific authority extraction during user loading.
 */
@Component
class PassthroughOidcAuthoritiesProvider : OidcAuthoritiesProvider {
    override val key: String = "passthrough"

    override fun getAuthorities(
        oidcUser: OidcUser,
        userRequest: OidcUserRequest,
    ): Set<GrantedAuthority> = oidcUser.authorities.toSet()
}
