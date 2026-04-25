package cz.grimir.wifimanager.captive.application.authorization.handler.command

import cz.grimir.wifimanager.captive.application.authorization.command.RemoveAuthorizationTokenCommand
import cz.grimir.wifimanager.captive.application.authorization.event.MacAuthorizationStateChangedEvent
import cz.grimir.wifimanager.captive.application.authorization.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.authorization.port.ModifyAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.shared.port.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import cz.grimir.wifimanager.captive.core.value.Device
import cz.grimir.wifimanager.shared.core.TicketId
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import java.time.Instant
import java.util.UUID

class RemoveAuthorizationTokenUsecaseTest {
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort = mock()
    private val modifyAuthorizationTokenPort: ModifyAuthorizationTokenPort = mock()
    private val captiveEventPublisher: CaptiveEventPublisher = mock()
    private val ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000601"))

    private val usecase =
        RemoveAuthorizationTokenUsecase(
            findAuthorizationTokenPort = findAuthorizationTokenPort,
            modifyAuthorizationTokenPort = modifyAuthorizationTokenPort,
            captiveEventPublisher = captiveEventPublisher,
        )

    @Test
    fun `removing token publishes one state change event with distinct device macs`() {
        given(findAuthorizationTokenPort.findByTicketId(ticketId)).willReturn(
            token(
                "aa:bb:cc:dd:ee:01",
                "aa:bb:cc:dd:ee:02",
                "aa:bb:cc:dd:ee:01",
            ),
        )

        usecase.remove(RemoveAuthorizationTokenCommand(ticketId))

        verify(modifyAuthorizationTokenPort).deleteByTicketId(ticketId)
        verify(captiveEventPublisher).publish(
            MacAuthorizationStateChangedEvent(
                listOf(
                    "aa:bb:cc:dd:ee:01",
                    "aa:bb:cc:dd:ee:02",
                ),
            ),
        )
    }

    private fun token(vararg macs: String): AuthorizationToken =
        AuthorizationToken(
            id = ticketId,
            accessCode = "ABCDEFGH",
            validUntil = Instant.parse("2025-01-01T11:00:00Z"),
            requireUserNameOnLogin = false,
            authorizedDevices = macs.map { Device(mac = it, displayName = null, deviceName = null) }.toMutableList(),
            kickedMacAddresses = mutableSetOf(),
        )
}
