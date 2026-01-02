package cz.grimir.wifimanager.user.application

import cz.grimir.wifimanager.shared.core.ResolveUserCommand
import cz.grimir.wifimanager.shared.core.RoleMappingInput
import cz.grimir.wifimanager.shared.core.UserProfileSnapshot
import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.shared.events.UserCreatedOrUpdatedEvent
import cz.grimir.wifimanager.user.application.ports.UserDirectoryRepository
import cz.grimir.wifimanager.user.application.ports.UserEventPublisher
import cz.grimir.wifimanager.user.application.ports.UserIdentityRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class UserDirectoryServiceTest {
    private val roleMapper = RoleMapper()

    @Test
    fun `creates new identity and publishes event`() {
        val repository = FakeRepository()
        val publisher = FakePublisher()
        val service = UserDirectoryService(repository, roleMapper, publisher)

        val command = command(displayName = "Alice", roles = setOf("WIFI_ADMIN"))

        val result = service.resolveOrCreate(command)

        assertEquals(1, repository.createIdentityCalls)
        assertEquals(0, repository.updateIdentityCalls)
        assertEquals(0, repository.updateLastLoginCalls)
        assertEquals(1, publisher.events.size)
        assertEquals(
            result.userId,
            publisher.events
                .single()
                .userId.id,
        )
    }

    @Test
    fun `keeps identity when profile unchanged and does not publish event`() {
        val existing = record(displayName = "Alice", email = "alice@example.com", roles = setOf(UserRole.WIFI_STAFF))
        val repository = FakeRepository(existing = existing)
        val publisher = FakePublisher()
        val service = UserDirectoryService(repository, roleMapper, publisher)

        val command =
            command(
                displayName = existing.displayName,
                email = existing.email,
                roles = setOf("WIFI_STAFF"),
            )

        service.resolveOrCreate(command)

        assertEquals(0, repository.createIdentityCalls)
        assertEquals(0, repository.updateIdentityCalls)
        assertEquals(1, repository.updateLastLoginCalls)
        assertEquals(0, publisher.events.size)
    }

    @Test
    fun `updates identity when profile changes and publishes event`() {
        val existing = record(displayName = "Alice", email = "alice@example.com", roles = setOf(UserRole.WIFI_STAFF))
        val repository = FakeRepository(existing = existing)
        val publisher = FakePublisher()
        val service = UserDirectoryService(repository, roleMapper, publisher)

        val command =
            command(
                displayName = "Alice Doe",
                email = "alice@example.com",
                roles = setOf("WIFI_STAFF", "WIFI_ADMIN"),
            )

        service.resolveOrCreate(command)

        assertEquals(0, repository.createIdentityCalls)
        assertEquals(1, repository.updateIdentityCalls)
        assertEquals(0, repository.updateLastLoginCalls)
        assertEquals(1, publisher.events.size)
    }

    @Test
    fun `handles concurrent insert by reloading identity without emitting event`() {
        val existing = record(displayName = "Alice", email = "alice@example.com", roles = setOf(UserRole.WIFI_STAFF))
        val repository =
            FakeRepository(existing = existing).apply {
                firstFindReturnsNull = true
                createReturnsNull = true
            }
        val publisher = FakePublisher()
        val service = UserDirectoryService(repository, roleMapper, publisher)

        val command =
            command(
                displayName = existing.displayName,
                email = existing.email,
                roles = setOf("WIFI_STAFF"),
            )

        service.resolveOrCreate(command)

        assertEquals(1, repository.createIdentityCalls)
        assertEquals(0, repository.updateIdentityCalls)
        assertEquals(1, repository.updateLastLoginCalls)
        assertEquals(0, publisher.events.size)
    }

    private fun command(
        displayName: String = "Alice",
        email: String = "alice@example.com",
        roles: Set<String> = setOf("WIFI_STAFF"),
    ): ResolveUserCommand =
        ResolveUserCommand(
            issuer = "issuer",
            subject = "subject",
            profile =
                UserProfileSnapshot(
                    displayName = displayName,
                    email = email,
                    pictureUrl = "https://example.com/avatar.png",
                ),
            roleMapping =
                RoleMappingInput(
                    roles = roles,
                    groups = emptySet(),
                ),
            loginAt = Instant.parse("2025-01-01T12:00:00Z"),
        )

    private fun record(
        displayName: String,
        email: String,
        roles: Set<UserRole>,
    ): UserIdentityRecord =
        UserIdentityRecord(
            identityId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            userId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
            issuer = "issuer",
            subject = "subject",
            displayName = displayName,
            email = email,
            pictureUrl = "https://example.com/avatar.png",
            roles = roles,
            createdAt = Instant.parse("2025-01-01T10:00:00Z"),
            updatedAt = Instant.parse("2025-01-01T10:00:00Z"),
            lastLoginAt = Instant.parse("2025-01-01T11:00:00Z"),
            inserted = false,
        )

    private class FakePublisher : UserEventPublisher {
        val events = mutableListOf<UserCreatedOrUpdatedEvent>()

        override fun publish(event: UserCreatedOrUpdatedEvent) {
            events.add(event)
        }
    }

    private class FakeRepository(
        existing: UserIdentityRecord? = null,
    ) : UserDirectoryRepository {
        var firstFindReturnsNull = false
        var createReturnsNull = false
        var createIdentityCalls = 0
        var updateIdentityCalls = 0
        var updateLastLoginCalls = 0

        private var record: UserIdentityRecord? = existing
        private var findCalls = 0

        override fun findByIssuerAndSubject(
            issuer: String,
            subject: String,
        ): UserIdentityRecord? {
            findCalls += 1
            if (firstFindReturnsNull && findCalls == 1) {
                return null
            }
            return record
        }

        override fun createIdentity(
            identityId: UUID,
            issuer: String,
            subject: String,
            displayName: String,
            email: String,
            pictureUrl: String?,
            roles: Set<UserRole>,
            loginAt: Instant,
        ): UserIdentityRecord? {
            createIdentityCalls += 1
            if (createReturnsNull) {
                return null
            }
            val created =
                UserIdentityRecord(
                    identityId = identityId,
                    userId = UUID.fromString("00000000-0000-0000-0000-000000000010"),
                    issuer = issuer,
                    subject = subject,
                    displayName = displayName,
                    email = email,
                    pictureUrl = pictureUrl,
                    roles = roles,
                    createdAt = loginAt,
                    updatedAt = loginAt,
                    lastLoginAt = loginAt,
                    inserted = true,
                )
            record = created
            return created
        }

        override fun updateIdentity(
            identityId: UUID,
            displayName: String,
            email: String,
            pictureUrl: String?,
            roles: Set<UserRole>,
            loginAt: Instant,
        ): UserIdentityRecord {
            updateIdentityCalls += 1
            val current = record ?: error("No identity to update")
            val updated =
                current.copy(
                    displayName = displayName,
                    email = email,
                    pictureUrl = pictureUrl,
                    roles = roles,
                    updatedAt = loginAt,
                    lastLoginAt = loginAt,
                    inserted = false,
                )
            record = updated
            return updated
        }

        override fun updateLastLoginAt(
            identityId: UUID,
            loginAt: Instant,
        ) {
            updateLastLoginCalls += 1
            val current = record ?: error("No identity to update")
            record = current.copy(lastLoginAt = loginAt)
        }
    }
}
