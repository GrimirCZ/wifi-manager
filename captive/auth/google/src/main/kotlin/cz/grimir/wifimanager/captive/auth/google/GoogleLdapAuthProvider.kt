package cz.grimir.wifimanager.captive.auth.google

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

class GoogleLdapAuthProvider(
    private val secureLdapClient: GoogleSecureLdapClient,
    private val directoryApiClient: GoogleDirectoryApiClient,
    private val userDirectoryClient: UserDirectoryClient,
    private val allowedDevicesByGroup: Map<String, Int>,
) : UserAuthProvider {
    private val logger = KotlinLogging.logger {}

    override fun authenticate(credentials: UserCredentials): UserAuthenticationResult? {
        val authenticated = secureLdapClient.authenticate(credentials)
        if (!authenticated) {
            return null
        }

        return try {
            val directoryUser = directoryApiClient.fetchUser(credentials.username)
            val groups = directoryApiClient.fetchGroups(directoryUser.primaryEmail)
            val subject = directoryUser.id

            val resolved =
                userDirectoryClient.resolveUser(
                    ResolveUserCommand(
                        issuer = ISSUER,
                        subject = subject,
                        profile =
                            UserProfileSnapshot(
                                displayName = directoryUser.fullName,
                                email = directoryUser.primaryEmail,
                                pictureUrl = directoryUser.thumbnailPhotoUrl,
                            ),
                        roleMapping = RoleMappingInput(),
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
                allowedDeviceCount = resolveAllowedDeviceCount(groups),
                groups = groups,
            )
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to resolve user for Google LDAP ${credentials.username}" }
            null
        }
    }

    private fun resolveAllowedDeviceCount(groups: Set<String>): Int {
        if (allowedDevicesByGroup.isEmpty()) {
            return 0
        }
        return groups
            .mapNotNull { allowedDevicesByGroup[it] }
            .maxOrNull()
            ?: 0
    }

    companion object {
        const val ISSUER: String = "https://accounts.google.com"
    }
}
