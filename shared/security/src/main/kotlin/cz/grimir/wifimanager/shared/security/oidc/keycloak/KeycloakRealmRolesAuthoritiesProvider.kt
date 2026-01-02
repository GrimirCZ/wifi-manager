package cz.grimir.wifimanager.shared.security.oidc.keycloak

import cz.grimir.wifimanager.shared.security.oidc.OidcAccessTokenDecoder
import cz.grimir.wifimanager.shared.security.oidc.OidcAuthoritiesProvider
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component
import java.util.LinkedHashSet
import kotlin.collections.get

/**
 * Keycloak authorities mapping:
 * - Reads realm roles from `realm_access.roles` claims (ID token/userinfo) when available.
 *
 * Notes:
 * - Prefer configuring Keycloak to include `realm_access.roles` in ID token/userinfo via a mapper.
 * - If roles are missing from claims, this falls back to decoding the access token JWT.
 */
@Component
class KeycloakRealmRolesAuthoritiesProvider(
    private val accessTokenDecoder: OidcAccessTokenDecoder,
) : OidcAuthoritiesProvider {
    override val key: String = "keycloak-realm-roles"

    override fun getAuthorities(
        oidcUser: OidcUser,
        userRequest: OidcUserRequest,
    ): Set<GrantedAuthority> {
        val mapped = LinkedHashSet<GrantedAuthority>()
        mapped.addAll(oidcUser.authorities)

        val claimRoles = extractRealmRoles(oidcUser.claims)
        val fallbackRoles =
            if (claimRoles.isNotEmpty()) {
                emptySet()
            } else {
                extractRealmRolesFromAccessToken(userRequest)
            }

        claimRoles
            .plus(fallbackRoles)
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
        val jwt =
            try {
                accessTokenDecoder.decode(userRequest)
            } catch (_: Exception) {
                null
            } ?: return emptySet()
        return extractRealmRoles(jwt.claims)
    }
}
