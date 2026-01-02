package cz.grimir.wifimanager.shared.security

import cz.grimir.wifimanager.shared.core.ResolveUserCommand
import cz.grimir.wifimanager.shared.core.RoleMappingInput
import cz.grimir.wifimanager.shared.core.UserDirectoryClient
import cz.grimir.wifimanager.shared.core.UserProfileSnapshot
import cz.grimir.wifimanager.shared.security.mvc.SessionUserIdentity
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class ResolveUserSuccessHandler(
    private val userDirectoryClient: UserDirectoryClient,
) : AuthenticationSuccessHandler {
    private val logger = KotlinLogging.logger {}
    private val delegate = SavedRequestAwareAuthenticationSuccessHandler()

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val oauth =
            authentication as? OAuth2AuthenticationToken
                ?: error("OAuth2AuthenticationToken is required for OIDC login")
        val oidcUser =
            oauth.principal as? OidcUser
                ?: error("OIDC principal is required for OIDC login")

        val issuer =
            oidcUser.issuer?.toString()
                ?: oidcUser.idToken?.issuer?.toString()
                ?: oidcUser.getClaimAsString("iss")
                ?: error("OIDC issuer claim missing")

        val subject =
            oidcUser.subject
                ?: oidcUser.getClaimAsString("sub")
                ?: error("OIDC subject (sub) not available")

        val email =
            oidcUser.getClaimAsString("email")
                ?: error("OIDC claim 'email' is required for UI login")

        val displayName =
            oidcUser.getClaimAsString("preferred_username")
                ?: oidcUser.getClaimAsString("name")
                ?: email

        val pictureUrl = oidcUser.getClaimAsString("picture")

        val roles = extractRoles(oidcUser, oauth)
        val groups = extractGroups(oidcUser)

        val result =
            try {
                userDirectoryClient.resolveUser(
                    ResolveUserCommand(
                        issuer = issuer,
                        subject = subject,
                        profile =
                            UserProfileSnapshot(
                                displayName = displayName,
                                email = email,
                                pictureUrl = pictureUrl,
                            ),
                        roleMapping =
                            RoleMappingInput(
                                roles = roles,
                                groups = groups,
                            ),
                    ),
                )
            } catch (ex: Exception) {
                logger.error(ex) {
                    "Failed to resolve user during OIDC login issuer=$issuer registrationId=${oauth.authorizedClientRegistrationId}"
                }
                SecurityContextHolder.clearContext()
                request.getSession(false)?.invalidate()
                response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Login failed while resolving your account. Please try again or contact support.",
                )
                return
            }

        val mappedAuthorities =
            result.roles
                .map { role -> SimpleGrantedAuthority("ROLE_${role.name}") }
                .toSet()

        val newAuth = OAuth2AuthenticationToken(oidcUser, mappedAuthorities, oauth.authorizedClientRegistrationId)
        newAuth.details = oauth.details
        SecurityContextHolder.getContext().authentication = newAuth

        request.session.setAttribute(
            SessionUserIdentity.SESSION_KEY,
            SessionUserIdentity(
                userId = result.userId,
                identityId = result.identityId,
                displayName = result.displayName,
                email = result.email,
                pictureUrl = result.pictureUrl,
                roles = result.roles,
            ),
        )

        delegate.onAuthenticationSuccess(request, response, newAuth)
    }

    private fun extractRoles(
        oidcUser: OidcUser,
        authentication: OAuth2AuthenticationToken,
    ): Set<String> {
        val realmRoles =
            (oidcUser.claims["realm_access"] as? Map<*, *>)
                ?.get("roles")
                ?.let(::extractStringCollection)
                .orEmpty()

        val claimRoles =
            extractStringCollection(oidcUser.claims["roles"]) +
                extractStringCollection(oidcUser.claims["role"]) +
                realmRoles

        val authorityRoles =
            authentication.authorities
                .mapNotNull { it.authority }
                .filter { it.startsWith("ROLE_") }
                .map { it.removePrefix("ROLE_") }

        return (claimRoles + authorityRoles).toSet()
    }

    private fun extractGroups(oidcUser: OidcUser): Set<String> = extractStringCollection(oidcUser.claims["groups"]).toSet()

    private fun extractStringCollection(value: Any?): List<String> =
        when (value) {
            is Collection<*> -> value.filterIsInstance<String>()
            is Array<*> -> value.filterIsInstance<String>()
            is String -> listOf(value)
            else -> emptyList()
        }
}
