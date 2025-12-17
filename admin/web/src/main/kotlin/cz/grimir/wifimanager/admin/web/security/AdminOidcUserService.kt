package cz.grimir.wifimanager.admin.web.security

import cz.grimir.wifimanager.admin.application.commands.UpsertUserFromLoginCommand
import cz.grimir.wifimanager.admin.application.model.UserRole
import cz.grimir.wifimanager.admin.application.usecases.commands.UpsertUserFromLoginUsecase
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
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
 * - Maps IdP roles/groups into Spring Security authorities (`ROLE_*`).
 */
@Component
class AdminOidcUserService(
    private val upsertUserFromLoginUsecase: UpsertUserFromLoginUsecase,
    private val authoritiesProviderRegistry: AdminAuthoritiesProviderRegistry,
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
            ?: displayName

        val firstName = oidcUser.getClaimAsString("given_name")
        val lastName = oidcUser.getClaimAsString("family_name")

        val authorities = authoritiesProviderRegistry.getAuthorities(oidcUser, userRequest)

        upsertUserFromLoginUsecase.upsert(
            UpsertUserFromLoginCommand(
                issuer = issuer,
                subject = oidcUser.subject,
                email = email,
                firstName = firstName,
                lastName = lastName,
                displayName = displayName,
                pictureUrl = pictureUrl,
                username = providerUsername,
                loginAt = Instant.now(),
                roles = authorities
                    .mapNotNull { it.authority?.removePrefix("ROLE_") }
                    .filter { UserRole.entries.any { role -> role.name == it } }
                    .map(UserRole::valueOf)
                    .toSet()
            ),
        )

        return DefaultOidcUser(authorities, oidcUser.idToken, oidcUser.userInfo, "sub")
    }
}
