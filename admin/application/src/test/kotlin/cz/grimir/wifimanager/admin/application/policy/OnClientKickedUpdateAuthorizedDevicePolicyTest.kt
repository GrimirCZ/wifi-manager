package cz.grimir.wifimanager.admin.application.policy

import cz.grimir.wifimanager.admin.application.ports.FindAuthorizedDevicePort
import cz.grimir.wifimanager.admin.application.ports.SaveAuthorizedDevicePort
import cz.grimir.wifimanager.admin.core.value.AuthorizedDevice
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.events.ClientKickedEvent
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
class OnClientKickedUpdateAuthorizedDevicePolicyTest {
    private val findAuthorizedDevicePort: FindAuthorizedDevicePort = mock()
    private val saveAuthorizedDevicePort: SaveAuthorizedDevicePort = mock()

    private val policy =
        OnClientKickedUpdateAuthorizedDevicePolicy(
            findAuthorizedDevicePort,
            saveAuthorizedDevicePort,
        )

    @Test
    fun `ignores event when device not found`() {
        val ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000030"))
        given(findAuthorizedDevicePort.findByMacAndTicketId("AA:BB:CC:DD:EE:FF", ticketId)).willReturn(null)

        policy.on(
            ClientKickedEvent(
                ticketId = ticketId,
                deviceMacAddress = "AA:BB:CC:DD:EE:FF",
                kickedAt = Instant.parse("2025-01-01T10:00:00Z"),
            ),
        )

        verifyNoInteractions(saveAuthorizedDevicePort)
    }

    @Test
    fun `ignores event when device already kicked`() {
        val ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000031"))
        val device =
            AuthorizedDevice(
                ticketId = ticketId,
                mac = "AA:BB:CC:DD:EE:FF",
                name = "phone",
                wasKicked = true,
            )
        given(findAuthorizedDevicePort.findByMacAndTicketId("AA:BB:CC:DD:EE:FF", ticketId)).willReturn(device)

        policy.on(
            ClientKickedEvent(
                ticketId = ticketId,
                deviceMacAddress = "AA:BB:CC:DD:EE:FF",
                kickedAt = Instant.parse("2025-01-01T10:00:00Z"),
            ),
        )

        verifyNoInteractions(saveAuthorizedDevicePort)
    }

    @Test
    fun `updates device when not kicked yet`() {
        val ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000032"))
        val device =
            AuthorizedDevice(
                ticketId = ticketId,
                mac = "AA:BB:CC:DD:EE:FF",
                name = "phone",
                wasKicked = false,
            )
        given(findAuthorizedDevicePort.findByMacAndTicketId("AA:BB:CC:DD:EE:FF", ticketId)).willReturn(device)

        policy.on(
            ClientKickedEvent(
                ticketId = ticketId,
                deviceMacAddress = "AA:BB:CC:DD:EE:FF",
                kickedAt = Instant.parse("2025-01-01T10:00:00Z"),
            ),
        )

        val deviceCaptor = argumentCaptor<AuthorizedDevice>()
        verify(saveAuthorizedDevicePort).save(deviceCaptor.capture())
        assertTrue(deviceCaptor.firstValue.wasKicked)
    }
}
