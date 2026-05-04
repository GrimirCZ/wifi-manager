package cz.grimir.wifimanager.captive.application.authorization.handler.command

import cz.grimir.wifimanager.captive.application.command.RevokeClientAccessCommand
import cz.grimir.wifimanager.captive.application.event.MacAuthorizationStateChangedEvent
import cz.grimir.wifimanager.captive.application.command.handler.RevokeClientAccessUsecase
import cz.grimir.wifimanager.captive.application.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.port.ModifyAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.port.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.events.ClientAccessRevokedEvent
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import java.time.Instant
import java.util.UUID

class RevokeClientAccessUsecaseTest {
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort = mock()
    private val modifyAuthorizationTokenPort: ModifyAuthorizationTokenPort = mock()
    private val eventPublisher: CaptiveEventPublisher = mock()
    private val timeProvider: TimeProvider = mock()
    private val ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000501"))
    private val mac = "aa:bb:cc:dd:ee:01"
    private val now = Instant.parse("2025-01-01T10:15:00Z")

    private val usecase =
        RevokeClientAccessUsecase(
            findAuthorizationTokenPort = findAuthorizationTokenPort,
            modifyAuthorizationTokenPort = modifyAuthorizationTokenPort,
            eventPublisher = eventPublisher,
            timeProvider = timeProvider,
        )

    @Test
    fun `ticket kick publishes client revoked and mac state change events`() {
        val token = token()
        given(findAuthorizationTokenPort.findByTicketId(ticketId)).willReturn(token)
        given(timeProvider.get()).willReturn(now)

        usecase.revoke(RevokeClientAccessCommand(ticketId, mac))

        verify(modifyAuthorizationTokenPort).save(token)
        verify(eventPublisher).publish(ClientAccessRevokedEvent(ticketId, mac, now))
        verify(eventPublisher).publish(MacAuthorizationStateChangedEvent(listOf(mac)))
    }

    private fun token(): AuthorizationToken =
        AuthorizationToken(
            id = ticketId,
            accessCode = "ABCDEFGH",
            validUntil = Instant.parse("2025-01-01T11:00:00Z"),
            requireUserNameOnLogin = false,
            authorizedDevices = mutableListOf(),
            kickedMacAddresses = mutableSetOf(),
        )
}
