package cz.grimir.wifimanager.captive.web.security

import cz.grimir.wifimanager.captive.application.command.UpsertNetworkUserOnLoginCommand
import cz.grimir.wifimanager.captive.application.command.handler.UpsertNetworkUserOnLoginUsecase
import cz.grimir.wifimanager.captive.application.command.model.UserAuthenticationResult
import cz.grimir.wifimanager.captive.application.command.model.UserCredentials
import cz.grimir.wifimanager.captive.application.port.UserAuthProviderPort
import cz.grimir.wifimanager.captive.application.query.ResolveNetworkUserLimitQuery
import cz.grimir.wifimanager.captive.application.query.handler.ResolveNetworkUserLimitUsecase
import cz.grimir.wifimanager.captive.application.query.model.NetworkUser
import cz.grimir.wifimanager.shared.application.identity.model.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.shared.security.mvc.SessionUserIdentity
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import java.time.Instant
import java.util.UUID

class CaptiveAuthSessionLoginHandlerTest {
    private val authProvider: UserAuthProviderPort = mock()
    private val upsertNetworkUserOnLoginUsecase: UpsertNetworkUserOnLoginUsecase = mock()
    private val resolveNetworkUserLimitUsecase: ResolveNetworkUserLimitUsecase = mock()
    private val handler =
        CaptiveAuthSessionLoginHandler(
            authProviders = listOf(authProvider),
            upsertNetworkUserOnLoginUsecase = upsertNetworkUserOnLoginUsecase,
            resolveNetworkUserLimitUsecase = resolveNetworkUserLimitUsecase,
        )

    @Test
    fun `successful login stores user identity but no device fingerprint binding`() {
        val request = mock<HttpServletRequest>()
        val session = mock<HttpSession>()
        val credentials = UserCredentials(username = "user", password = "secret")
        val authResult = UserAuthenticationResult(identity = identity(), allowedDeviceCount = 2)
        given(authProvider.authenticate(credentials)).willReturn(authResult)
        given(
            upsertNetworkUserOnLoginUsecase.upsert(UpsertNetworkUserOnLoginCommand(authResult.identity, authResult.allowedDeviceCount)),
        ).willReturn(networkUser())
        given(resolveNetworkUserLimitUsecase.resolve(ResolveNetworkUserLimitQuery(networkUser()))).willReturn(2)
        given(request.session).willReturn(session)

        val result = handler.login(credentials, request)

        assertTrue(result.success)
        verify(request).changeSessionId()
        val sessionIdentityCaptor = argumentCaptor<SessionUserIdentity>()
        verify(session).setAttribute(org.mockito.kotlin.eq(SessionUserIdentity.SESSION_KEY), sessionIdentityCaptor.capture())
        assertEquals(authResult.identity.userId.id, sessionIdentityCaptor.firstValue.userId)
        verifyNoMoreInteractions(session)
    }

    @Test
    fun `login fails when authenticated user does not match reauth owner`() {
        val request = mock<HttpServletRequest>()
        val credentials = UserCredentials(username = "user", password = "secret")
        val authResult = UserAuthenticationResult(identity = identity(), allowedDeviceCount = 2)
        given(authProvider.authenticate(credentials)).willReturn(authResult)

        val result =
            handler.login(
                credentials = credentials,
                request = request,
                expectedUserId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000999")),
            )

        assertFalse(result.success)
        assertEquals(LdapLoginFailureReason.DEVICE_OWNERSHIP_MISMATCH, result.failureReason)
        verify(request, never()).changeSessionId()
    }

    @Test
    fun `login fails when resolved device limit is zero`() {
        val request = mock<HttpServletRequest>()
        val credentials = UserCredentials(username = "user", password = "secret")
        val authResult = UserAuthenticationResult(identity = identity(), allowedDeviceCount = 2)
        given(authProvider.authenticate(credentials)).willReturn(authResult)
        given(
            upsertNetworkUserOnLoginUsecase.upsert(UpsertNetworkUserOnLoginCommand(authResult.identity, authResult.allowedDeviceCount)),
        ).willReturn(networkUser())
        given(resolveNetworkUserLimitUsecase.resolve(ResolveNetworkUserLimitQuery(networkUser()))).willReturn(0)

        val result = handler.login(credentials, request)

        assertFalse(result.success)
        assertEquals(LdapLoginFailureReason.NO_DEVICE_ACCESS, result.failureReason)
        verify(request, never()).changeSessionId()
    }

    private fun identity(): UserIdentitySnapshot =
        UserIdentitySnapshot(
            userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000111")),
            identityId = UUID.fromString("00000000-0000-0000-0000-000000000222"),
            displayName = "Test User",
            email = "user@example.com",
            pictureUrl = null,
            roles = setOf(UserRole.WIFI_ADMIN),
        )

    private fun networkUser(): NetworkUser =
        NetworkUser(
            userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000111")),
            identityId = UUID.fromString("00000000-0000-0000-0000-000000000222"),
            allowedDeviceCount = 2,
            adminOverrideLimit = null,
            createdAt = Instant.parse("2025-01-01T10:00:00Z"),
            updatedAt = Instant.parse("2025-01-01T10:00:00Z"),
            lastLoginAt = Instant.parse("2025-01-01T10:00:00Z"),
        )
}
