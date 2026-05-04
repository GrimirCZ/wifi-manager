package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.command.handler.ScrubDeauthorizedCaptiveDeviceUsecase
import cz.grimir.wifimanager.captive.application.event.MacAuthorizationStateChangedEvent
import cz.grimir.wifimanager.captive.application.port.RouterAgentPort
import cz.grimir.wifimanager.captive.application.support.ClientAccessAuthorizationResolver
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class MacAuthorizationStateChangedEventListenerTest {
    private val resolver: ClientAccessAuthorizationResolver = mock()
    private val routerAgentPort: RouterAgentPort = mock()
    private val scrubDeauthorizedCaptiveDeviceUsecase: ScrubDeauthorizedCaptiveDeviceUsecase = mock()

    private val listener =
        MacAuthorizationStateChangedEventListener(
            clientAccessAuthorizationResolver = resolver,
            routerAgentPort = routerAgentPort,
            scrubDeauthorizedCaptiveDeviceUsecase = scrubDeauthorizedCaptiveDeviceUsecase,
        )

    @Test
    fun `revokes each normalized unauthorized mac once`() {
        listener.on(
            MacAuthorizationStateChangedEvent(
                listOf(
                    "AA-BB-CC-DD-EE-01",
                    "aa:bb:cc:dd:ee:01",
                    "AA-BB-CC-DD-EE-02",
                ),
            ),
        )

        verify(routerAgentPort).revokeClientAccess(listOf("aa:bb:cc:dd:ee:01"))
        verify(routerAgentPort).revokeClientAccess(listOf("aa:bb:cc:dd:ee:02"))
        verify(scrubDeauthorizedCaptiveDeviceUsecase).scrubIfEligible("aa:bb:cc:dd:ee:01")
        verify(scrubDeauthorizedCaptiveDeviceUsecase).scrubIfEligible("aa:bb:cc:dd:ee:02")
    }

    @Test
    fun `does not revoke when any authorization source still authorizes mac`() {
        val mac = "aa:bb:cc:dd:ee:01"
        given(resolver.isAuthorizedByActiveTicketDevice(mac)).willReturn(true)

        listener.on(MacAuthorizationStateChangedEvent(listOf(mac)))

        verify(routerAgentPort, never()).revokeClientAccess(listOf(mac))
        verify(scrubDeauthorizedCaptiveDeviceUsecase, never()).scrubIfEligible(mac)
    }

    @Test
    fun `revokes but delegates cleanup eligibility decision for pending reauth mac`() {
        val mac = "aa:bb:cc:dd:ee:01"
        given(resolver.isAuthorizedByActiveTicketDevice(mac)).willReturn(false)
        given(resolver.isAuthorizedByAccountDevice(mac)).willReturn(false)
        given(resolver.isAuthorizedByAllowedMac(mac)).willReturn(false)

        listener.on(MacAuthorizationStateChangedEvent(listOf(mac)))

        verify(routerAgentPort).revokeClientAccess(listOf(mac))
        verify(scrubDeauthorizedCaptiveDeviceUsecase).scrubIfEligible(mac)
    }
}
