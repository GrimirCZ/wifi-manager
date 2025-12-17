package cz.grimir.wifimanager.admin.web.security.keycloak

import cz.grimir.wifimanager.admin.web.security.AdminAuthoritiesProvider
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component
import java.util.LinkedHashSet
import kotlin.collections.get

/**
 * Keycloak authorities mapping:
 * - Reads realm roles from `realm_access.roles`.
 *
 * Notes:
 * - Keycloak typically puts roles into the *access token* (JWT) rather than the ID token/userinfo.
 * - This implementation decodes the access token JWT to extract realm roles when needed.
 */
@Component
class KeycloakRealmRolesAuthoritiesProvider(
    private val accessTokenDecoder: AdminAccessTokenDecoder,
) : AdminAuthoritiesProvider {
    override val key: String = "keycloak-realm-roles"

    override fun getAuthorities(
        oidcUser: OidcUser,
        userRequest: OidcUserRequest,
    ): Set<GrantedAuthority> {
        val mapped = oidcUser.authorities.toMutableSet()

        extractRealmRoles(oidcUser.claims)
            .plus(extractRealmRolesFromAccessToken(userRequest))
            .map { role -> if (role.startsWith("ROLE_")) role else "ROLE_$role" }
            .map(::SimpleGrantedAuthority)
            .forEach(mapped::add)

        return mapped
    }

    private fun extractRealmRoles(claims: Map<String, Any?>): Set<String> {
        val realmAccess = claims["realm_access"] as? Map<*, *> ?: return emptySet()
        val roles = realmAccess["roles"] as? Collection<*> ?: return emptySet()
        return roles.filterIsInstance<String>().toSet()
    }

    private fun extractRealmRolesFromAccessToken(userRequest: OidcUserRequest): Set<String> {
        val jwt = accessTokenDecoder.decode(userRequest) ?: return emptySet()
        return extractRealmRoles(jwt.claims)
    }
}
