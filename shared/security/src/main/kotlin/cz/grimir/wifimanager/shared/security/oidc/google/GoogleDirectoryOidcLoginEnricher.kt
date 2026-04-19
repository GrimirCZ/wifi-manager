package cz.grimir.wifimanager.shared.security.oidc.google

import cz.grimir.wifimanager.shared.application.identity.google.GoogleDirectoryApiClient
import cz.grimir.wifimanager.shared.core.RoleMappingInput
import cz.grimir.wifimanager.shared.core.UserProfileSnapshot
import cz.grimir.wifimanager.shared.security.UiSecurityProperties
import cz.grimir.wifimanager.shared.security.oidc.OidcLoginEnricher
import cz.grimir.wifimanager.shared.security.oidc.OidcLoginEnrichment
import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component

@Component
@Profile("google")
class GoogleDirectoryOidcLoginEnricher(
    private val directoryApiClient: GoogleDirectoryApiClient,
    private val securityProperties: UiSecurityProperties,
) : OidcLoginEnricher {
    override fun supports(issuer: String): Boolean = issuer == GOOGLE_ISSUER

    override fun enrich(oidcUser: OidcUser): OidcLoginEnrichment {
        val subject =
            oidcUser.subject
                ?: oidcUser.getClaimAsString("sub")
                ?: error("OIDC subject (sub) not available")

        val directoryUser = directoryApiClient.fetchUser(subject)
        val groups = directoryApiClient.fetchGroups(subject)
        val mappedRoles = groups.mapNotNull { securityProperties.google.roleByGroup()[it] }.toSet()

        return OidcLoginEnrichment(
            profile =
                UserProfileSnapshot(
                    displayName = directoryUser.fullName,
                    email = directoryUser.primaryEmail,
                    pictureUrl = directoryUser.thumbnailPhotoUrl,
                ),
            roleMapping = RoleMappingInput(roles = mappedRoles),
            groups = groups,
        )
    }

    companion object {
        const val GOOGLE_ISSUER: String = "https://accounts.google.com"
    }
}
