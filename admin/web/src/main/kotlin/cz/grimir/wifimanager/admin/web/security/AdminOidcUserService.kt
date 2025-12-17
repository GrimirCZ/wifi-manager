package cz.grimir.wifimanager.admin.web.security

import cz.grimir.wifimanager.admin.application.commands.UpsertUserFromLoginCommand
import cz.grimir.wifimanager.admin.application.usecases.commands.UpsertUserFromLoginUsecase
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * OIDC user service for the admin UI.
 *
 * Responsibilities:
 * - Reads basic user profile claims from the IdP (email/name/picture).
 * - Creates or updates the local user record on each successful login.
 * - Maps Keycloak realm roles (`realm_access.roles`) to Spring Security authorities (`ROLE_*`).
 */
@Component
class AdminOidcUserService(
    private val upsertUserFromLoginUsecase: UpsertUserFromLoginUsecase,
) : OAuth2UserService<OidcUserRequest, OidcUser> {
    private val delegate = OidcUserService()

    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val oidcUser = delegate.loadUser(userRequest)

        val issuer =
            oidcUser.issuer?.toString()
                ?: userRequest.clientRegistration.providerDetails.issuerUri
                ?: userRequest.clientRegistration.registrationId

        val email =
            oidcUser.getClaimAsString("email")
                ?: error("OIDC claim 'email' is required for admin login")

        val displayName =
            oidcUser.getClaimAsString("preferred_username")
                ?: oidcUser.getClaimAsString("name")
                ?: email

        val pictureUrl = oidcUser.getClaimAsString("picture")
        val providerUsername = oidcUser.getClaimAsString("preferred_username")

        upsertUserFromLoginUsecase.upsert(
            UpsertUserFromLoginCommand(
                issuer = issuer,
                subject = oidcUser.subject,
                email = email,
                displayName = displayName,
                pictureUrl = pictureUrl,
                emailAtProvider = email,
                providerUsername = providerUsername,
                loginAt = Instant.now(),
            ),
        )

        return DefaultOidcUser(mapAuthorities(oidcUser), oidcUser.idToken, oidcUser.userInfo, "sub")
    }

    /**
     * Maps Keycloak realm roles into Spring Security authorities.
     *
     * Example:
     * - `realm_access.roles = ["ADMIN", "USER"]` becomes `[ROLE_ADMIN, ROLE_USER]`.
     */
    private fun mapAuthorities(oidcUser: OidcUser): Set<GrantedAuthority> {
        val mapped = LinkedHashSet<GrantedAuthority>()
        mapped.addAll(oidcUser.authorities)

        val realmAccess = oidcUser.claims["realm_access"] as? Map<*, *> ?: return mapped
        val roles = realmAccess["roles"] as? Collection<*> ?: return mapped

        roles
            .filterIsInstance<String>()
            .map { role -> if (role.startsWith("ROLE_")) role else "ROLE_$role" }
            .map(::SimpleGrantedAuthority)
            .forEach(mapped::add)

        return mapped
    }
}
