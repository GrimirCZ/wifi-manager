package cz.grimir.wifimanager.captive.auth.keycloak

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import cz.grimir.wifimanager.captive.application.user.UserAuthProvider
import cz.grimir.wifimanager.captive.application.user.UserAuthenticationResult
import cz.grimir.wifimanager.captive.application.user.UserCredentials
import cz.grimir.wifimanager.shared.application.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.ResolveUserCommand
import cz.grimir.wifimanager.shared.core.RoleMappingInput
import cz.grimir.wifimanager.shared.core.UserDirectoryClient
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserProfileSnapshot
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body

class KeycloakPasswordGrantAuthProvider(
    private val restClient: RestClient,
    private val properties: KeycloakPasswordGrantProperties,
    private val userDirectoryClient: UserDirectoryClient,
) : UserAuthProvider {
    private val logger = KotlinLogging.logger {}
    private val allowedDevicesByRole = properties.allowedDevicesByRole()

    override fun authenticate(credentials: UserCredentials): UserAuthenticationResult? {
        val token = requestToken(credentials) ?: return null
        val userInfo = requestUserInfo(token.accessToken) ?: return null
        val subject = userInfo.subject
        if (subject.isBlank()) {
            logger.warn { "Keycloak userinfo response missing subject for ${credentials.username}" }
            return null
        }
        val email = userInfo.email?.takeIf { it.isNotBlank() }
        if (email == null) {
            logger.warn { "Keycloak userinfo response missing email for ${credentials.username}" }
            return null
        }

        val roles = userInfo.realmAccess?.roles ?: emptySet()
        val displayName =
            userInfo.name?.takeIf { it.isNotBlank() }
                ?: userInfo.preferredUsername?.takeIf { it.isNotBlank() }
                ?: email

        return try {
            val resolved =
                userDirectoryClient.resolveUser(
                    ResolveUserCommand(
                        issuer = properties.issuerUri,
                        subject = subject,
                        profile =
                            UserProfileSnapshot(
                                displayName = displayName,
                                email = email,
                                pictureUrl = userInfo.picture,
                            ),
                        roleMapping = RoleMappingInput(roles = roles),
                    ),
                )

            UserAuthenticationResult(
                identity =
                    UserIdentitySnapshot(
                        userId = UserId(resolved.userId),
                        identityId = resolved.identityId,
                        displayName = resolved.displayName,
                        email = resolved.email,
                        pictureUrl = resolved.pictureUrl,
                        roles = resolved.roles,
                    ),
                allowedDeviceCount = resolveAllowedDeviceCount(roles),
                groups = roles,
            )
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to resolve user for Keycloak ${credentials.username}" }
            null
        }
    }

    private fun requestToken(credentials: UserCredentials): TokenResponse? {
        val payload =
            LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "password")
                add("client_id", properties.clientId)
                add("client_secret", properties.clientSecret)
                add("username", credentials.username)
                add("password", credentials.password)
                add("scope", "openid profile email roles")
            }

        return try {
            restClient
                .post()
                .uri("/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(payload)
                .retrieve()
                .body<TokenResponse>()
        } catch (ex: RestClientResponseException) {
            if (ex.statusCode == HttpStatus.BAD_REQUEST || ex.statusCode == HttpStatus.FORBIDDEN) {
                logger.info { "Keycloak authentication rejected for ${credentials.username}" }
            } else {
                logger.error(ex) { "Keycloak token request failed for ${credentials.username}" }
            }
            null
        }
    }

    private fun requestUserInfo(accessToken: String): UserInfoResponse? =
        try {
            restClient
                .get()
                .uri("/protocol/openid-connect/userinfo")
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body<UserInfoResponse>()
        } catch (ex: RestClientResponseException) {
            logger.error(ex) { "Keycloak userinfo request failed" }
            null
        }

    private fun resolveAllowedDeviceCount(roles: Set<String>): Int {
        if (allowedDevicesByRole.isEmpty()) {
            return 0
        }
        return roles
            .mapNotNull { allowedDevicesByRole[it] }
            .maxOrNull()
            ?: 0
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TokenResponse(
        @get:JsonProperty("access_token")
        val accessToken: String = "",
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class UserInfoResponse(
        @get:JsonProperty("sub")
        val subject: String,
        val email: String? = null,
        val name: String? = null,
        @get:JsonProperty("preferred_username")
        val preferredUsername: String? = null,
        val picture: String? = null,
        @get:JsonProperty("realm_access")
        val realmAccess: RealmAccess? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class RealmAccess(
        val roles: Set<String> = emptySet(),
    )
}
