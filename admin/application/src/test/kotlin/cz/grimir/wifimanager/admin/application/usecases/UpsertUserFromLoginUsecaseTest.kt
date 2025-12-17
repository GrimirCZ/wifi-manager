package cz.grimir.wifimanager.admin.application.usecases

import cz.grimir.wifimanager.admin.application.commands.UpsertUserFromLoginCommand
import cz.grimir.wifimanager.admin.application.exceptions.UserWithThisEmailAlreadyExistsForAnotherProvider
import cz.grimir.wifimanager.admin.application.model.User
import cz.grimir.wifimanager.admin.application.model.UserIdentity
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
import java.time.Instant
import java.util.UUID

class UpsertUserFromLoginUsecaseTest {
    @Test
    fun `creates user and identity on first login`() {
        val repo = InMemoryUserRepo()
        val usecase = repo.usecase()
        val loginAt = Instant.parse("2025-01-01T10:00:00Z")

        val userId =
            usecase.upsert(
                UpsertUserFromLoginCommand(
                    issuer = "https://issuer.example",
                    subject = "sub-1",
                    email = "a@example.com",
                    displayName = "Alice",
                    pictureUrl = "https://img.example/a.png",
                    loginAt = loginAt,
                ),
            )

        assertNotNull(repo.identitiesByIssuerSubject["https://issuer.example|sub-1"])
        assertNotNull(repo.usersById[userId.id])
    }

    @Test
    fun `updates user and identity on subsequent login`() {
        val repo = InMemoryUserRepo()
        val existingUserId = UserId(UUID.randomUUID())
        val existingIdentityId = UUID.randomUUID()

        repo.usersById[existingUserId.id] =
            User(
                id = existingUserId,
                email = "old@example.com",
                displayName = "Old Name",
                pictureUrl = null,
                isActive = true,
                createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2025-01-01T00:00:00Z"),
                lastLoginAt = Instant.parse("2025-01-01T00:00:00Z"),
            )
        repo.identitiesByIssuerSubject["https://issuer.example|sub-1"] =
            UserIdentity(
                id = existingIdentityId,
                userId = existingUserId,
                issuer = "https://issuer.example",
                subject = "sub-1",
                emailAtProvider = "old@example.com",
                providerUsername = "old-username",
                createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                lastLoginAt = Instant.parse("2025-01-01T00:00:00Z"),
            )

        val usecase = repo.usecase()
        val loginAt = Instant.parse("2025-01-02T10:00:00Z")

        val userId =
            usecase.upsert(
                UpsertUserFromLoginCommand(
                    issuer = "https://issuer.example",
                    subject = "sub-1",
                    email = "new@example.com",
                    displayName = "New Name",
                    pictureUrl = "https://img.example/new.png",
                    loginAt = loginAt,
                ),
            )

        assertEquals(existingUserId.id, userId.id)

        val savedUser = repo.usersById[existingUserId.id]!!
        assertEquals(loginAt, savedUser.lastLoginAt)
        assertEquals(loginAt, savedUser.updatedAt)

        val savedIdentity = repo.identitiesByIssuerSubject["https://issuer.example|sub-1"]!!
        assertEquals(loginAt, savedIdentity.lastLoginAt)
        assertEquals("new@example.com", savedIdentity.emailAtProvider)
        assertEquals("old-username", savedIdentity.providerUsername)
    }

    @Test
    fun `throws when creating user with existing email`() {
        val repo = InMemoryUserRepo()
        repo.usersById[UUID.fromString("00000000-0000-0000-0000-000000000001")] =
            User(
                id = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                email = "existing@example.com",
                displayName = "Existing",
                pictureUrl = null,
                isActive = true,
                createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2025-01-01T00:00:00Z"),
                lastLoginAt = Instant.parse("2025-01-01T00:00:00Z"),
            )

        val usecase = repo.usecase()

        assertThrows(UserWithThisEmailAlreadyExistsForAnotherProvider::class.java) {
            usecase.upsert(
                UpsertUserFromLoginCommand(
                    issuer = "https://issuer.example",
                    subject = "new-subject",
                    email = "existing@example.com",
                    displayName = "New",
                    pictureUrl = null,
                    loginAt = Instant.parse("2025-01-02T10:00:00Z"),
                ),
            )
        }
    }
}

private class InMemoryUserRepo :
    FindUserPort,
    SaveUserPort,
    FindUserIdentityPort,
    SaveUserIdentityPort {
    val usersById = linkedMapOf<UUID, User>()
    val identitiesByIssuerSubject = linkedMapOf<String, UserIdentity>()

    override fun findById(id: UserId): User? = usersById[id.id]

    override fun findByEmail(email: String): User? =
        usersById.values.firstOrNull { it.email.equals(email, ignoreCase = true) }

    override fun save(user: User): User {
        usersById[user.id.id] = user
        return user
    }

    override fun findByIssuerAndSubject(
        issuer: String,
        subject: String,
    ): UserIdentity? = identitiesByIssuerSubject["$issuer|$subject"]

    override fun save(identity: UserIdentity): UserIdentity {
        identitiesByIssuerSubject["${identity.issuer}|${identity.subject}"] = identity
        return identity
    }

    fun usecase(): UpsertUserFromLoginUsecase =
        UpsertUserFromLoginUsecase(
            findUserIdentityPort = this,
            findUserPort = this,
            saveUserPort = this,
            saveUserIdentityPort = this,
        )
}
