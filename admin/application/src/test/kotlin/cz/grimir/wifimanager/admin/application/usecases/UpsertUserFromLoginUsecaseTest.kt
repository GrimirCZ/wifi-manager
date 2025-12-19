package cz.grimir.wifimanager.admin.application.usecases

import cz.grimir.wifimanager.admin.application.commands.UpsertUserFromLoginCommand
import cz.grimir.wifimanager.admin.application.exceptions.UserWithThisEmailAlreadyExistsForAnotherProvider
import cz.grimir.wifimanager.admin.application.model.User
import cz.grimir.wifimanager.admin.application.model.UserIdentity
import cz.grimir.wifimanager.admin.application.model.UserRole
import cz.grimir.wifimanager.admin.application.ports.FindUserIdentityPort
import cz.grimir.wifimanager.admin.application.ports.FindUserPort
import cz.grimir.wifimanager.admin.application.ports.SaveUserIdentityPort
import cz.grimir.wifimanager.admin.application.ports.SaveUserPort
import cz.grimir.wifimanager.admin.application.usecases.commands.UpsertUserFromLoginUsecase
import cz.grimir.wifimanager.shared.core.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UpsertUserFromLoginUsecaseTest {
    private val findUserIdentityPort: FindUserIdentityPort = mock()

    private val findUserPort: FindUserPort = mock()

    private val saveUserPort: SaveUserPort = mock()

    private val saveUserIdentityPort: SaveUserIdentityPort = mock()

    private val usecase =
        UpsertUserFromLoginUsecase(findUserIdentityPort, findUserPort, saveUserPort, saveUserIdentityPort)

    @Test
    fun `creates user and identity on first login`() {
        val loginAt = Instant.parse("2025-01-01T10:00:00Z")

        given(findUserIdentityPort.findByIssuerAndSubject("https://issuer.example", "sub-1")).willReturn(null)
        given(findUserIdentityPort.findByEmail("a@example.com")).willReturn(null)
        given(saveUserPort.save(any<User>())).willAnswer { invocation -> invocation.arguments[0] as User }

        val userId =
            usecase.upsert(
                UpsertUserFromLoginCommand(
                    issuer = "https://issuer.example",
                    subject = "sub-1",
                    email = "a@example.com",
                    firstName = "Alice",
                    lastName = "Example",
                    displayName = "Alice Example",
                    pictureUrl = "https://img.example/a.png",
                    username = "alice",
                    roles = setOf(UserRole.WIFI_STAFF),
                    loginAt = loginAt,
                ),
            )

        assertNotNull(userId)

        val userCaptor = argumentCaptor<User>()
        verify(saveUserPort).save(userCaptor.capture())
        assertEquals(loginAt, userCaptor.firstValue.createdAt)
        assertEquals(loginAt, userCaptor.firstValue.updatedAt)
        assertEquals(loginAt, userCaptor.firstValue.lastLoginAt)

        val identityCaptor = argumentCaptor<UserIdentity>()
        verify(saveUserIdentityPort).save(identityCaptor.capture())
        assertEquals(userCaptor.firstValue.id, identityCaptor.firstValue.userId)
        assertEquals("https://issuer.example", identityCaptor.firstValue.issuer)
        assertEquals("sub-1", identityCaptor.firstValue.subject)
        assertEquals("a@example.com", identityCaptor.firstValue.email)
    }

    @Test
    fun `updates user and identity on subsequent login`() {
        val existingUserId = UserId(UUID.randomUUID())
        val existingIdentityId = UUID.randomUUID()
        val loginAt = Instant.parse("2025-01-02T10:00:00Z")

        val existingUser =
            User(
                id = existingUserId,
                createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2025-01-01T00:00:00Z"),
                lastLoginAt = Instant.parse("2025-01-01T00:00:00Z"),
            )
        val existingIdentity =
            UserIdentity(
                id = existingIdentityId,
                userId = existingUserId,
                issuer = "https://issuer.example",
                subject = "sub-1",
                email = "old@example.com",
                username = "old-username",
                firstName = "Old",
                lastName = "Name",
                pictureUrl = null,
                createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                lastLoginAt = Instant.parse("2025-01-01T00:00:00Z"),
                roles = setOf(UserRole.WIFI_STAFF),
            )

        given(findUserIdentityPort.findByIssuerAndSubject("https://issuer.example", "sub-1")).willReturn(existingIdentity)
        given(findUserPort.findById(existingUserId)).willReturn(existingUser)
        given(saveUserPort.save(any<User>())).willAnswer { invocation -> invocation.arguments[0] as User }
        given(saveUserIdentityPort.save(any<UserIdentity>())).willAnswer { invocation ->
            invocation.arguments[0] as UserIdentity
        }

        val userId =
            usecase.upsert(
                UpsertUserFromLoginCommand(
                    issuer = "https://issuer.example",
                    subject = "sub-1",
                    email = "new@example.com",
                    firstName = "New",
                    lastName = "Name",
                    displayName = "New Name",
                    pictureUrl = "https://img.example/new.png",
                    username = "old-username",
                    roles = setOf(UserRole.WIFI_ADMIN),
                    loginAt = loginAt,
                ),
            )

        assertEquals(existingUserId.id, userId.id)

        val savedUserCaptor = argumentCaptor<User>()
        verify(saveUserPort).save(savedUserCaptor.capture())
        assertEquals(loginAt, savedUserCaptor.firstValue.lastLoginAt)
        assertEquals(loginAt, savedUserCaptor.firstValue.updatedAt)

        val savedIdentityCaptor = argumentCaptor<UserIdentity>()
        verify(saveUserIdentityPort).save(savedIdentityCaptor.capture())
        assertEquals(loginAt, savedIdentityCaptor.firstValue.lastLoginAt)
        assertEquals("new@example.com", savedIdentityCaptor.firstValue.email)
        assertEquals("old-username", savedIdentityCaptor.firstValue.username)
        assertEquals("New", savedIdentityCaptor.firstValue.firstName)
        assertEquals("Name", savedIdentityCaptor.firstValue.lastName)
        assertEquals("https://img.example/new.png", savedIdentityCaptor.firstValue.pictureUrl)
        assertEquals(setOf(UserRole.WIFI_ADMIN), savedIdentityCaptor.firstValue.roles)
    }

    @Test
    fun `throws when creating user with existing email`() {
        val existingIdentity =
            UserIdentity(
                id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
                issuer = "https://issuer.example",
                subject = "sub-1",
                email = "existing@example.com",
                username = "existing",
                firstName = "Existing",
                lastName = "User",
                pictureUrl = null,
                createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                lastLoginAt = Instant.parse("2025-01-01T00:00:00Z"),
                roles = setOf(UserRole.WIFI_STAFF),
            )

        given(findUserIdentityPort.findByIssuerAndSubject("https://issuer.example", "new-subject")).willReturn(null)
        given(findUserIdentityPort.findByEmail("existing@example.com")).willReturn(existingIdentity)

        assertThrows(UserWithThisEmailAlreadyExistsForAnotherProvider::class.java) {
            usecase.upsert(
                UpsertUserFromLoginCommand(
                    issuer = "https://issuer.example",
                    subject = "new-subject",
                    email = "existing@example.com",
                    firstName = "New",
                    lastName = "User",
                    displayName = "New User",
                    pictureUrl = null,
                    username = "new-user",
                    roles = emptySet(),
                    loginAt = Instant.parse("2025-01-02T10:00:00Z"),
                ),
            )
        }

        verify(saveUserPort, never()).save(any<User>())
        verify(saveUserIdentityPort, never()).save(any<UserIdentity>())
    }
}
