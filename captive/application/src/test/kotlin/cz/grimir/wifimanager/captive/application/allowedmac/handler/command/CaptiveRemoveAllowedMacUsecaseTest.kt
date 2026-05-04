package cz.grimir.wifimanager.captive.application.allowedmac.handler.command

import cz.grimir.wifimanager.captive.application.command.handler.CaptiveRemoveAllowedMacUsecase
import cz.grimir.wifimanager.captive.application.query.model.AllowedMac
import cz.grimir.wifimanager.captive.application.port.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.port.AllowedMacWritePort
import cz.grimir.wifimanager.captive.application.event.MacAuthorizationStateChangedEvent
import cz.grimir.wifimanager.captive.application.port.CaptiveEventPublisher
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

class CaptiveRemoveAllowedMacUsecaseTest {
    private val allowedMacReadPort: AllowedMacReadPort = mock()
    private val allowedMacWritePort: AllowedMacWritePort = mock()
    private val captiveEventPublisher: CaptiveEventPublisher = mock()
    private val mac = "aa:bb:cc:dd:ee:01"

    private val usecase =
        CaptiveRemoveAllowedMacUsecase(
            allowedMacReadPort = allowedMacReadPort,
            allowedMacWritePort = allowedMacWritePort,
            captiveEventPublisher = captiveEventPublisher,
        )

    @Test
    fun `removing allowed mac publishes state change event`() {
        given(allowedMacReadPort.findByMac(mac)).willReturn(AllowedMac(mac = mac, validUntil = null))

        usecase.remove(mac)

        verify(allowedMacWritePort).deleteByMac(mac)
        verify(captiveEventPublisher).publish(MacAuthorizationStateChangedEvent(listOf(mac)))
    }
}
