package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.commands.KickDeviceCommand
import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.ports.SaveTicketPort
import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import cz.grimir.wifimanager.admin.core.exceptions.UserNotAllowedToKickDevice
import cz.grimir.wifimanager.shared.application.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.shared.events.ClientKickedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class KickClientUsecaseTest {
    private val findTicketPort: FindTicketPort = mock()
    private val saveTicketPort: SaveTicketPort = mock()
    private val eventPublisher: AdminEventPublisher = mock()
    private val fixedInstant = Instant.parse("2025-01-01T12:34:56Z")
    private val timeProvider = TimeProvider { fixedInstant }

    private val usecase =
        KickClientUsecase(
            findTicketPort,
            saveTicketPort,
            eventPublisher,
            timeProvider,
        )

    @Test
    fun `records kicked mac and publishes event`() {
        val ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000010"))
        val user =
            buildUser(
                UUID.fromString("00000000-0000-0000-0000-000000000011"),
                setOf(UserRole.WIFI_STAFF),
            )
        val ticket =
            Ticket(
                id = ticketId,
                accessCode = "ABC123",
                createdAt = Instant.parse("2025-01-01T09:00:00Z"),
                validUntil = Instant.parse("2025-01-01T10:00:00Z"),
                wasCanceled = false,
                authorId = user.userId,
            )

        given(findTicketPort.findById(ticketId)).willReturn(ticket)

        usecase.kick(
            KickDeviceCommand(
                ticketId = ticketId,
                deviceMacAddress = "AA:BB:CC:DD:EE:FF",
                user = user,
            ),
        )

        val ticketCaptor = argumentCaptor<Ticket>()
        verify(saveTicketPort).save(ticketCaptor.capture())
        assertTrue(ticketCaptor.firstValue.kickedMacAddresses.contains("AA:BB:CC:DD:EE:FF"))

        val eventCaptor = argumentCaptor<ClientKickedEvent>()
        verify(eventPublisher).publish(eventCaptor.capture())
        assertEquals(ticketId, eventCaptor.firstValue.ticketId)
        assertEquals("AA:BB:CC:DD:EE:FF", eventCaptor.firstValue.deviceMacAddress)
        assertEquals(fixedInstant, eventCaptor.firstValue.kickedAt)
    }

    @Test
    fun `allows admin to kick devices on other users ticket`() {
        val ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000030"))
        val admin =
            buildUser(
                UUID.fromString("00000000-0000-0000-0000-000000000031"),
                setOf(UserRole.WIFI_ADMIN),
            )
        val ticket =
            Ticket(
                id = ticketId,
                accessCode = "ABC123",
                createdAt = Instant.parse("2025-01-01T09:00:00Z"),
                validUntil = Instant.parse("2025-01-01T10:00:00Z"),
                wasCanceled = false,
                authorId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000032")),
            )

        given(findTicketPort.findById(ticketId)).willReturn(ticket)

        usecase.kick(
            KickDeviceCommand(
                ticketId = ticketId,
                deviceMacAddress = "AA:BB:CC:DD:EE:FF",
                user = admin,
            ),
        )

        verify(saveTicketPort).save(ticket)
    }

    @Test
    fun `throws when user is not allowed to kick devices`() {
        val ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000040"))
        val user =
            buildUser(
                UUID.fromString("00000000-0000-0000-0000-000000000041"),
                setOf(UserRole.WIFI_STAFF),
            )
        val ticket =
            Ticket(
                id = ticketId,
                accessCode = "ABC123",
                createdAt = Instant.parse("2025-01-01T09:00:00Z"),
                validUntil = Instant.parse("2025-01-01T10:00:00Z"),
                wasCanceled = false,
                authorId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000042")),
            )

        given(findTicketPort.findById(ticketId)).willReturn(ticket)

        assertThrows(UserNotAllowedToKickDevice::class.java) {
            usecase.kick(
                KickDeviceCommand(
                    ticketId = ticketId,
                    deviceMacAddress = "AA:BB:CC:DD:EE:FF",
                    user = user,
                ),
            )
        }
    }

    @Test
    fun `throws when ticket is missing`() {
        val ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000020"))
        val user =
            buildUser(
                UUID.fromString("00000000-0000-0000-0000-000000000021"),
                setOf(UserRole.WIFI_STAFF),
            )
        given(findTicketPort.findById(ticketId)).willReturn(null)

        assertThrows(IllegalStateException::class.java) {
            usecase.kick(
                KickDeviceCommand(
                    ticketId = ticketId,
                    deviceMacAddress = "AA:BB:CC:DD:EE:FF",
                    user = user,
                ),
            )
        }
    }

    private fun buildUser(
        userId: UUID,
        roles: Set<UserRole>,
    ): UserIdentitySnapshot =
        UserIdentitySnapshot(
            userId = UserId(userId),
            identityId = UUID.randomUUID(),
            displayName = "user",
            email = "user@example.com",
            pictureUrl = null,
            roles = roles,
        )
}
