package cz.grimir.wifimanager.captive.application.networkuserdevice.handler.command

import cz.grimir.wifimanager.captive.application.event.MacAuthorizationStateChangedEvent
import cz.grimir.wifimanager.captive.application.command.handler.RemoveNetworkUserDeviceUsecase
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.captive.application.port.CaptiveEventPublisher
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceRemovedEvent
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import java.time.Instant
import java.util.UUID

class RemoveNetworkUserDeviceUsecaseTest {
    private val networkUserDeviceWritePort: NetworkUserDeviceWritePort = mock()
    private val captiveEventPublisher: CaptiveEventPublisher = mock()
    private val timeProvider: TimeProvider = mock()
    private val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000301"))
    private val mac = "aa:bb:cc:dd:ee:01"
    private val now = Instant.parse("2025-01-01T10:15:00Z")

    private val usecase =
        RemoveNetworkUserDeviceUsecase(
            networkUserDeviceWritePort = networkUserDeviceWritePort,
            captiveEventPublisher = captiveEventPublisher,
            timeProvider = timeProvider,
        )

    @Test
    fun `removing account authorization publishes state change event`() {
        given(timeProvider.get()).willReturn(now)

        usecase.remove(userId, mac)

        verify(networkUserDeviceWritePort).delete(userId, mac)
        verify(captiveEventPublisher).publish(NetworkUserDeviceRemovedEvent(userId, mac, now))
        verify(captiveEventPublisher).publish(MacAuthorizationStateChangedEvent(listOf(mac)))
    }
}
