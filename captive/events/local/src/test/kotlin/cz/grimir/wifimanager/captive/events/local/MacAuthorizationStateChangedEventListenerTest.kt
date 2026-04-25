package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.authorization.event.MacAuthorizationStateChangedEvent
import cz.grimir.wifimanager.captive.application.authorization.support.ClientAccessAuthorizationResolver
import cz.grimir.wifimanager.captive.application.integration.routeragent.port.RouterAgentPort
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class MacAuthorizationStateChangedEventListenerTest {
    private val resolver: ClientAccessAuthorizationResolver = mock()
    private val routerAgentPort: RouterAgentPort = mock()

    private val listener =
        MacAuthorizationStateChangedEventListener(
            clientAccessAuthorizationResolver = resolver,
            routerAgentPort = routerAgentPort,
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
    }

    @Test
    fun `does not revoke when any authorization source still authorizes mac`() {
        val mac = "aa:bb:cc:dd:ee:01"
        given(resolver.isAuthorizedByActiveTicketDevice(mac)).willReturn(true)

        listener.on(MacAuthorizationStateChangedEvent(listOf(mac)))

        verify(routerAgentPort, never()).revokeClientAccess(listOf(mac))
    }
}
