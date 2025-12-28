package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.commands.AddAuthorizedDeviceCommand
import cz.grimir.wifimanager.admin.application.ports.FindAuthorizedDevicePort
import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.ports.SaveAuthorizedDevicePort
import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import cz.grimir.wifimanager.admin.core.value.AuthorizedDevice
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verifyNoInteractions
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AddAuthorizedDeviceUsecaseTest {
    private val findTicketPort: FindTicketPort = mock()
    private val findAuthorizedDevicePort: FindAuthorizedDevicePort = mock()
    private val saveAuthorizedDevicePort: SaveAuthorizedDevicePort = mock()

    private val usecase =
        AddAuthorizedDeviceUsecase(
            findTicketPort,
            findAuthorizedDevicePort,
            saveAuthorizedDevicePort,
        )

    @Test
    fun `creates device with access not revoked`() {
        val ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
        val ticket =
            Ticket(
                id = ticketId,
                accessCode = "ABC123",
                createdAt = Instant.parse("2025-01-01T09:00:00Z"),
                validUntil = Instant.parse("2025-01-01T10:00:00Z"),
                wasCanceled = false,
                authorId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000003")),
                kickedMacAddresses = mutableSetOf(),
            )

        given(findTicketPort.findById(ticketId)).willReturn(ticket)
        given(findAuthorizedDevicePort.findByMacAndTicketId("AA:BB:CC:DD:EE:FF", ticketId)).willReturn(null)

        usecase.add(
            AddAuthorizedDeviceCommand(
                ticketId = ticketId,
                deviceMacAddress = "AA:BB:CC:DD:EE:FF",
                deviceName = "phone",
            ),
        )

        val deviceCaptor = argumentCaptor<AuthorizedDevice>()
        verify(saveAuthorizedDevicePort).save(deviceCaptor.capture())
        assertFalse(deviceCaptor.firstValue.wasAccessRevoked)
        assertEquals("phone", deviceCaptor.firstValue.name)
    }

    @Test
    fun `does not mark device as access revoked when ticket contains mac`() {
        val ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
        val ticket =
            Ticket(
                id = ticketId,
                accessCode = "ABC123",
                createdAt = Instant.parse("2025-01-01T09:00:00Z"),
                validUntil = Instant.parse("2025-01-01T10:00:00Z"),
                wasCanceled = false,
                authorId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000003")),
                kickedMacAddresses = mutableSetOf("AA:BB:CC:DD:EE:FF"),
            )

        given(findTicketPort.findById(ticketId)).willReturn(ticket)
        given(findAuthorizedDevicePort.findByMacAndTicketId("AA:BB:CC:DD:EE:FF", ticketId)).willReturn(null)

        usecase.add(
            AddAuthorizedDeviceCommand(
                ticketId = ticketId,
                deviceMacAddress = "AA:BB:CC:DD:EE:FF",
                deviceName = "phone",
            ),
        )

        val deviceCaptor = argumentCaptor<AuthorizedDevice>()
        verify(saveAuthorizedDevicePort).save(deviceCaptor.capture())
        assertFalse(deviceCaptor.firstValue.wasAccessRevoked)
        assertEquals("phone", deviceCaptor.firstValue.name)
    }

    @Test
    fun `updates existing device and preserves access revoked state`() {
        val ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000004"))
        val ticket =
            Ticket(
                id = ticketId,
                accessCode = "XYZ999",
                createdAt = Instant.parse("2025-01-01T09:00:00Z"),
                validUntil = Instant.parse("2025-01-01T10:00:00Z"),
                wasCanceled = false,
                authorId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000005")),
                kickedMacAddresses = mutableSetOf("11:22:33:44:55:66"),
            )
        val existingDevice =
            AuthorizedDevice(
                ticketId = ticketId,
                mac = "11:22:33:44:55:66",
                name = "old",
                wasAccessRevoked = true,
            )

        given(findTicketPort.findById(ticketId)).willReturn(ticket)
        given(findAuthorizedDevicePort.findByMacAndTicketId("11:22:33:44:55:66", ticketId)).willReturn(existingDevice)

        usecase.add(
            AddAuthorizedDeviceCommand(
                ticketId = ticketId,
                deviceMacAddress = "11:22:33:44:55:66",
                deviceName = "new",
            ),
        )

        val deviceCaptor = argumentCaptor<AuthorizedDevice>()
        verify(saveAuthorizedDevicePort).save(deviceCaptor.capture())
        assertEquals("new", deviceCaptor.firstValue.name)
        assertTrue(deviceCaptor.firstValue.wasAccessRevoked)
    }
}
