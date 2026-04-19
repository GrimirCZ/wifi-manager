package cz.grimir.wifimanager.shared.security.oidc

import cz.grimir.wifimanager.shared.core.RoleMappingInput
import cz.grimir.wifimanager.shared.core.UserProfileSnapshot
import org.springframework.security.oauth2.core.oidc.user.OidcUser

interface OidcLoginEnricher {
    fun supports(issuer: String): Boolean

    fun enrich(oidcUser: OidcUser): OidcLoginEnrichment
}

data class OidcLoginEnrichment(
    val profile: UserProfileSnapshot,
    val roleMapping: RoleMappingInput,
    val groups: Set<String> = emptySet(),
)
