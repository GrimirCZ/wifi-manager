package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.commands.UpsertAllowedMacCommand
import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ports.FindAllowedMacPort
import cz.grimir.wifimanager.admin.application.ports.SaveAllowedMacPort
import cz.grimir.wifimanager.admin.core.value.AllowedMac
import cz.grimir.wifimanager.shared.application.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verifyNoMoreInteractions
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminUpsertAllowedMacUsecaseTest {
    private val findAllowedMacPort: FindAllowedMacPort = mock()
    private val saveAllowedMacPort: SaveAllowedMacPort = mock()
    private val eventPublisher: AdminEventPublisher = mock()
    private val timeProvider: TimeProvider = mock()

    private val usecase =
        AdminUpsertAllowedMacUsecase(
            findAllowedMacPort,
            saveAllowedMacPort,
            eventPublisher,
            timeProvider,
        )

    @Test
    fun `overwrites note when mac already exists`() {
        val now = Instant.parse("2025-01-01T09:00:00Z")
        val existing =
            AllowedMac(
                mac = "aa:bb:cc:dd:ee:ff",
                hostname = "lab-printer",
                ownerUserId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                ownerDisplayName = "Owner",
                ownerEmail = "owner@example.com",
                note = "old note",
                validUntil = null,
                createdAt = now,
                updatedAt = now,
            )

        val user =
            UserIdentitySnapshot(
                userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
                identityId = UUID.fromString("00000000-0000-0000-0000-000000000003"),
                displayName = "Admin",
                email = "admin@example.com",
                pictureUrl = null,
                roles = emptySet(),
            )

        given(timeProvider.get()).willReturn(now)
        given(findAllowedMacPort.findByMac("aa:bb:cc:dd:ee:ff")).willReturn(existing)

        usecase.upsert(
            UpsertAllowedMacCommand(
                macAddress = "aa:bb:cc:dd:ee:ff",
                hostname = "lab-printer",
                note = "new note",
                validityDuration = null,
                user = user,
            ),
        )

        val captor = argumentCaptor<AllowedMac>()
        verify(saveAllowedMacPort).save(captor.capture())
        assertEquals("new note", captor.firstValue.note)
    }

    @Test
    fun `updates owner when mac already exists`() {
        val now = Instant.parse("2025-01-01T09:00:00Z")
        val existing =
            AllowedMac(
                mac = "aa:bb:cc:dd:ee:ff",
                hostname = "lab-printer",
                ownerUserId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                ownerDisplayName = "Old Owner",
                ownerEmail = "old@example.com",
                note = "",
                validUntil = null,
                createdAt = now,
                updatedAt = now,
            )

        val user =
            UserIdentitySnapshot(
                userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
                identityId = UUID.fromString("00000000-0000-0000-0000-000000000003"),
                displayName = "New Owner",
                email = "new@example.com",
                pictureUrl = null,
                roles = emptySet(),
            )

        given(timeProvider.get()).willReturn(now)
        given(findAllowedMacPort.findByMac("aa:bb:cc:dd:ee:ff")).willReturn(existing)

        usecase.upsert(
            UpsertAllowedMacCommand(
                macAddress = "aa:bb:cc:dd:ee:ff",
                hostname = null,
                note = "",
                validityDuration = null,
                user = user,
            ),
        )

        val captor = argumentCaptor<AllowedMac>()
        verify(saveAllowedMacPort).save(captor.capture())
        assertEquals(user.userId, captor.firstValue.ownerUserId)
        assertEquals(user.displayName, captor.firstValue.ownerDisplayName)
        assertEquals(user.email, captor.firstValue.ownerEmail)
    }

    @Test
    fun `creates new mac with duration`() {
        val now = Instant.parse("2025-01-01T09:00:00Z")
        val user =
            UserIdentitySnapshot(
                userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
                identityId = UUID.fromString("00000000-0000-0000-0000-000000000011"),
                displayName = "Admin",
                email = "admin@example.com",
                pictureUrl = null,
                roles = emptySet(),
            )

        given(timeProvider.get()).willReturn(now)
        given(findAllowedMacPort.findByMac("aa:bb:cc:dd:ee:11")).willReturn(null)

        usecase.upsert(
            UpsertAllowedMacCommand(
                macAddress = "aa:bb:cc:dd:ee:11",
                hostname = "camera",
                note = "lobby camera",
                validityDuration = java.time.Duration.ofMinutes(30),
                user = user,
            ),
        )

        val captor = argumentCaptor<AllowedMac>()
        verify(saveAllowedMacPort).save(captor.capture())
        assertEquals(now.plus(java.time.Duration.ofMinutes(30)), captor.firstValue.validUntil)
        assertEquals("lobby camera", captor.firstValue.note)
        assertEquals("camera", captor.firstValue.hostname)
        assertEquals(now, captor.firstValue.createdAt)
        assertEquals(now, captor.firstValue.updatedAt)
    }

    @Test
    fun `keeps existing hostname when command hostname is null`() {
        val now = Instant.parse("2025-01-01T09:00:00Z")
        val existing =
            AllowedMac(
                mac = "aa:bb:cc:dd:ee:ff",
                hostname = "printer",
                ownerUserId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                ownerDisplayName = "Owner",
                ownerEmail = "owner@example.com",
                note = "",
                validUntil = null,
                createdAt = now,
                updatedAt = now,
            )

        val user =
            UserIdentitySnapshot(
                userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
                identityId = UUID.fromString("00000000-0000-0000-0000-000000000003"),
                displayName = "Admin",
                email = "admin@example.com",
                pictureUrl = null,
                roles = emptySet(),
            )

        given(timeProvider.get()).willReturn(now)
        given(findAllowedMacPort.findByMac("aa:bb:cc:dd:ee:ff")).willReturn(existing)

        usecase.upsert(
            UpsertAllowedMacCommand(
                macAddress = "aa:bb:cc:dd:ee:ff",
                hostname = null,
                note = "",
                validityDuration = null,
                user = user,
            ),
        )

        val captor = argumentCaptor<AllowedMac>()
        verify(saveAllowedMacPort).save(captor.capture())
        assertEquals("printer", captor.firstValue.hostname)
    }

    @Test
    fun `sets validUntil to null when duration is null`() {
        val now = Instant.parse("2025-01-01T09:00:00Z")
        val existing =
            AllowedMac(
                mac = "aa:bb:cc:dd:ee:ff",
                hostname = "printer",
                ownerUserId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                ownerDisplayName = "Owner",
                ownerEmail = "owner@example.com",
                note = "",
                validUntil = now.plus(java.time.Duration.ofMinutes(10)),
                createdAt = now,
                updatedAt = now,
            )

        val user =
            UserIdentitySnapshot(
                userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
                identityId = UUID.fromString("00000000-0000-0000-0000-000000000003"),
                displayName = "Admin",
                email = "admin@example.com",
                pictureUrl = null,
                roles = emptySet(),
            )

        given(timeProvider.get()).willReturn(now)
        given(findAllowedMacPort.findByMac("aa:bb:cc:dd:ee:ff")).willReturn(existing)

        usecase.upsert(
            UpsertAllowedMacCommand(
                macAddress = "aa:bb:cc:dd:ee:ff",
                hostname = null,
                note = "",
                validityDuration = null,
                user = user,
            ),
        )

        val captor = argumentCaptor<AllowedMac>()
        verify(saveAllowedMacPort).save(captor.capture())
        assertEquals(null, captor.firstValue.validUntil)
    }

    @Test
    fun `publishes event with updated owner fields`() {
        val now = Instant.parse("2025-01-01T09:00:00Z")
        val existing =
            AllowedMac(
                mac = "aa:bb:cc:dd:ee:ff",
                hostname = "printer",
                ownerUserId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                ownerDisplayName = "Old Owner",
                ownerEmail = "old@example.com",
                note = "",
                validUntil = null,
                createdAt = now,
                updatedAt = now,
            )

        val user =
            UserIdentitySnapshot(
                userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
                identityId = UUID.fromString("00000000-0000-0000-0000-000000000003"),
                displayName = "New Owner",
                email = "new@example.com",
                pictureUrl = null,
                roles = emptySet(),
            )

        given(timeProvider.get()).willReturn(now)
        given(findAllowedMacPort.findByMac("aa:bb:cc:dd:ee:ff")).willReturn(existing)

        usecase.upsert(
            UpsertAllowedMacCommand(
                macAddress = "aa:bb:cc:dd:ee:ff",
                hostname = null,
                note = "",
                validityDuration = null,
                user = user,
            ),
        )

        val eventCaptor = argumentCaptor<cz.grimir.wifimanager.shared.events.AllowedMacUpsertedEvent>()
        verify(eventPublisher).publish(eventCaptor.capture())
        assertEquals(user.userId, eventCaptor.firstValue.ownerUserId)
        assertEquals(user.displayName, eventCaptor.firstValue.ownerDisplayName)
        assertEquals(user.email, eventCaptor.firstValue.ownerEmail)
        verifyNoMoreInteractions(eventPublisher)
    }
}
