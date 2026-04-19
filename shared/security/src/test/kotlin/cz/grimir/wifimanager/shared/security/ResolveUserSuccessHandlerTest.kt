package cz.grimir.wifimanager.shared.security

import cz.grimir.wifimanager.shared.application.identity.google.GoogleDirectoryApiClient
import cz.grimir.wifimanager.shared.core.ResolveUserCommand
import cz.grimir.wifimanager.shared.core.ResolveUserResult
import cz.grimir.wifimanager.shared.core.UserDirectoryClient
import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.shared.security.mvc.SessionUserIdentity
import cz.grimir.wifimanager.shared.security.oidc.google.GoogleDirectoryOidcLoginEnricher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.never
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.beans.factory.ObjectProvider
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import java.time.Instant
import java.util.UUID

class ResolveUserSuccessHandlerTest {
    private val userDirectoryClient: UserDirectoryClient = mock()
    private val directoryApiClient: GoogleDirectoryApiClient = mock()
    private val securityProperties =
        UiSecurityProperties(
            oidcRegistrationId = "admin",
            authoritiesProviderByRegistration = mapOf("admin" to "passthrough"),
            google =
                UiSecurityProperties.GoogleSecurityProperties(
                    roleByGroup = "wifi-admins@example.com=WIFI_ADMIN;wifi-staff@example.com=WIFI_STAFF",
                ),
        )
    private val googleOidcLoginEnricher = GoogleDirectoryOidcLoginEnricher(directoryApiClient, securityProperties)
    private val handler =
        ResolveUserSuccessHandler(
            userDirectoryClient = userDirectoryClient,
            oidcLoginEnricherProvider = objectProvider(googleOidcLoginEnricher),
        )

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `google login resolves mapped roles from directory groups`() {
        given(
            directoryApiClient.fetchUser("google-sub-123"),
        ).willReturn(
            GoogleDirectoryApiClient.GoogleDirectoryUser(
                id = "google-sub-123",
                primaryEmail = "alice@example.com",
                fullName = "Alice Doe",
                givenName = "Alice",
                familyName = "Doe",
                thumbnailPhotoUrl = "https://example.com/avatar.png",
            ),
        )
        given(directoryApiClient.fetchGroups("google-sub-123"))
            .willReturn(setOf("wifi-admins@example.com", "wifi-staff@example.com"))
        given(userDirectoryClient.resolveUser(org.mockito.kotlin.any())).willReturn(
            resolveUserResult(setOf(UserRole.WIFI_ADMIN, UserRole.WIFI_STAFF)),
        )
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        handler.onAuthenticationSuccess(request, response, googleAuthentication())

        val commandCaptor = argumentCaptor<ResolveUserCommand>()
        verify(userDirectoryClient).resolveUser(commandCaptor.capture())
        assertEquals("https://accounts.google.com", commandCaptor.firstValue.issuer)
        assertEquals("google-sub-123", commandCaptor.firstValue.subject)
        assertEquals("Alice Doe", commandCaptor.firstValue.profile.displayName)
        assertEquals("alice@example.com", commandCaptor.firstValue.profile.email)
        assertEquals(setOf("WIFI_ADMIN", "WIFI_STAFF"), commandCaptor.firstValue.roleMapping.roles)
        assertEquals(emptySet<String>(), commandCaptor.firstValue.roleMapping.groups)
        val sessionIdentity = request.getSession(false)!!.getAttribute(SessionUserIdentity.SESSION_KEY) as SessionUserIdentity
        assertEquals(setOf(UserRole.WIFI_ADMIN, UserRole.WIFI_STAFF), sessionIdentity.roles)
    }

    @Test
    fun `google login keeps zero roles when no groups are mapped`() {
        given(
            directoryApiClient.fetchUser("google-sub-123"),
        ).willReturn(
            GoogleDirectoryApiClient.GoogleDirectoryUser(
                id = "google-sub-123",
                primaryEmail = "alice@example.com",
                fullName = "Alice Doe",
                givenName = "Alice",
                familyName = "Doe",
                thumbnailPhotoUrl = null,
            ),
        )
        given(directoryApiClient.fetchGroups("google-sub-123"))
            .willReturn(setOf("unmapped@example.com"))
        given(userDirectoryClient.resolveUser(org.mockito.kotlin.any())).willReturn(
            resolveUserResult(emptySet()),
        )

        handler.onAuthenticationSuccess(MockHttpServletRequest(), MockHttpServletResponse(), googleAuthentication())

        val commandCaptor = argumentCaptor<ResolveUserCommand>()
        verify(userDirectoryClient).resolveUser(commandCaptor.capture())
        assertEquals(emptySet<String>(), commandCaptor.firstValue.roleMapping.roles)
    }

    @Test
    fun `google login fails when directory lookup fails`() {
        given(directoryApiClient.fetchUser("google-sub-123"))
            .willThrow(RuntimeException("boom"))
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        handler.onAuthenticationSuccess(request, response, googleAuthentication())

        assertEquals(500, response.status)
        assertNull(request.getSession(false))
        verify(userDirectoryClient, never()).resolveUser(org.mockito.kotlin.any())
    }

    @Test
    fun `non google login continues using oidc claims`() {
        given(userDirectoryClient.resolveUser(org.mockito.kotlin.any())).willReturn(
            resolveUserResult(setOf(UserRole.WIFI_ADMIN)),
        )

        handler.onAuthenticationSuccess(MockHttpServletRequest(), MockHttpServletResponse(), keycloakAuthentication())

        val commandCaptor = argumentCaptor<ResolveUserCommand>()
        verify(userDirectoryClient).resolveUser(commandCaptor.capture())
        assertEquals("alice@example.com", commandCaptor.firstValue.profile.email)
        assertEquals("alice", commandCaptor.firstValue.profile.displayName)
        assertEquals(setOf("WIFI_ADMIN"), commandCaptor.firstValue.roleMapping.roles)
        assertEquals(setOf("team-a"), commandCaptor.firstValue.roleMapping.groups)
        verify(directoryApiClient, never()).fetchUser(org.mockito.kotlin.any())
    }

    @Test
    fun `login continues without google enricher when google profile beans are absent`() {
        val handlerWithoutGoogle =
            ResolveUserSuccessHandler(
                userDirectoryClient = userDirectoryClient,
                oidcLoginEnricherProvider = emptyObjectProvider(),
            )
        given(userDirectoryClient.resolveUser(org.mockito.kotlin.any())).willReturn(
            resolveUserResult(setOf(UserRole.WIFI_ADMIN)),
        )

        handlerWithoutGoogle.onAuthenticationSuccess(MockHttpServletRequest(), MockHttpServletResponse(), googleAuthentication())

        val commandCaptor = argumentCaptor<ResolveUserCommand>()
        verify(userDirectoryClient).resolveUser(commandCaptor.capture())
        assertEquals("claim@example.com", commandCaptor.firstValue.profile.email)
        assertEquals("Claim Name", commandCaptor.firstValue.profile.displayName)
        assertEquals(emptySet<String>(), commandCaptor.firstValue.roleMapping.roles)
        assertEquals(emptySet<String>(), commandCaptor.firstValue.roleMapping.groups)
    }

    private fun googleAuthentication(): OAuth2AuthenticationToken =
        authentication(
            registrationId = "admin",
            issuer = "https://accounts.google.com",
            subject = "google-sub-123",
            claims =
                mapOf(
                    "email" to "claim@example.com",
                    "name" to "Claim Name",
                    "picture" to "https://example.com/claim.png",
                ),
        )

    private fun keycloakAuthentication(): OAuth2AuthenticationToken =
        authentication(
            registrationId = "admin",
            issuer = "https://keycloak.example.test/realms/wifimanager",
            subject = "keycloak-sub-123",
            claims =
                mapOf(
                    "email" to "alice@example.com",
                    "preferred_username" to "alice",
                    "groups" to listOf("team-a"),
                    "roles" to listOf("WIFI_ADMIN"),
                ),
            authorities = setOf(SimpleGrantedAuthority("OIDC_USER")),
        )

    private fun authentication(
        registrationId: String,
        issuer: String,
        subject: String,
        claims: Map<String, Any>,
        authorities: Set<SimpleGrantedAuthority> = setOf(SimpleGrantedAuthority("OIDC_USER")),
    ): OAuth2AuthenticationToken {
        val idToken =
            OidcIdToken(
                "token",
                Instant.parse("2025-01-01T10:00:00Z"),
                Instant.parse("2025-01-01T11:00:00Z"),
                claims + mapOf("iss" to issuer, "sub" to subject),
            )
        val oidcUser = DefaultOidcUser(authorities, idToken, OidcUserInfo(claims + mapOf("iss" to issuer, "sub" to subject)), "sub")
        return OAuth2AuthenticationToken(oidcUser, authorities, registrationId)
    }

    private fun resolveUserResult(roles: Set<UserRole>): ResolveUserResult =
        ResolveUserResult(
            userId = UUID.fromString("00000000-0000-0000-0000-000000000101"),
            identityId = UUID.fromString("00000000-0000-0000-0000-000000000202"),
            displayName = "Alice Doe",
            email = "alice@example.com",
            pictureUrl = "https://example.com/avatar.png",
            roles = roles,
        )

    private fun <T : Any> objectProvider(value: T): ObjectProvider<T> =
        object : ObjectProvider<T> {
            override fun getObject(vararg args: Any?): T = value

            override fun getIfAvailable(): T = value

            override fun getIfUnique(): T = value

            override fun getObject(): T = value
        }

    private fun <T : Any> emptyObjectProvider(): ObjectProvider<T> =
        object : ObjectProvider<T> {
            override fun getObject(vararg args: Any?): T = error("No bean available")

            override fun getIfAvailable(): T? = null

            override fun getIfUnique(): T? = null

            override fun getObject(): T = error("No bean available")
        }
}
