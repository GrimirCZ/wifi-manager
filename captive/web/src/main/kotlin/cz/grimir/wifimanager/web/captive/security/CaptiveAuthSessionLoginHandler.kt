package cz.grimir.wifimanager.web.captive.security

import cz.grimir.wifimanager.captive.application.usecase.commands.UpsertNetworkUserOnLoginUsecase
import cz.grimir.wifimanager.captive.application.usecase.queries.ResolveNetworkUserLimitUsecase
import cz.grimir.wifimanager.captive.application.user.UserAuthProvider
import cz.grimir.wifimanager.captive.application.user.UserCredentials
import cz.grimir.wifimanager.shared.security.mvc.SessionUserIdentity
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CaptiveAuthSessionLoginHandler(
    authProviders: List<UserAuthProvider>,
    private val upsertNetworkUserOnLoginUsecase: UpsertNetworkUserOnLoginUsecase,
    private val resolveNetworkUserLimitUsecase: ResolveNetworkUserLimitUsecase,
) {
    private val logger = KotlinLogging.logger {}

    init {
        if (authProviders.isEmpty()) {
            logger.warn { "No UserAuthProvider configured, captive authentication will not be available." }
        }
        if (authProviders.size > 1) {
            error("Multiple captive UserAuthProviders are not supported!")
        }
    }

    private val authProvider = authProviders.firstOrNull()

    fun login(
        credentials: UserCredentials,
        request: HttpServletRequest,
    ): LdapLoginResult {
        if (authProvider == null) {
            logger.warn { "No UserAuthProvider configured, cannot authenticate user ${credentials.username}" }
            return LdapLoginResult(success = false, failureReason = LdapLoginFailureReason.INVALID_CREDENTIALS)
        }

        val result = authProvider.authenticate(credentials)
        if (result == null) {
            logger.info { "User authentication failed for ${credentials.username}" }
            return LdapLoginResult(success = false, failureReason = LdapLoginFailureReason.INVALID_CREDENTIALS)
        }

        val networkUser = upsertNetworkUserOnLoginUsecase.upsert(result.identity, result.allowedDeviceCount)
        val effectiveLimit = resolveNetworkUserLimitUsecase.resolve(networkUser)
        if (effectiveLimit <= 0) {
            return LdapLoginResult(success = false, failureReason = LdapLoginFailureReason.NO_DEVICE_ACCESS)
        }

        val sessionIdentity =
            SessionUserIdentity(
                userId = result.identity.userId.id,
                identityId = result.identity.identityId,
                displayName = result.identity.displayName,
                email = result.identity.email,
                pictureUrl = result.identity.pictureUrl,
                roles = result.identity.roles,
            )

        request.changeSessionId()
        request.session.setAttribute(SessionUserIdentity.SESSION_KEY, sessionIdentity)

        val authorities =
            result.identity.roles
                .map { role -> SimpleGrantedAuthority("ROLE_${role.name}") }
                .toSet()
        val authentication = UsernamePasswordAuthenticationToken(result.identity, null, authorities)
        SecurityContextHolder.getContext().authentication = authentication

        return LdapLoginResult(success = true)
    }
}
