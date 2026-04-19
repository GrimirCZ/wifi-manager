package cz.grimir.wifimanager.captive.routeragent.grpc

import cz.grimir.wifimanager.captive.application.allowedmac.port.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.authorization.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.shared.events.AuthorizedClientObservedEvent
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.context.ApplicationEventPublisher

class RouterAgentGrpcServiceTest {
    private val hub: RouterAgentHub = mock()
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort = mock()
    private val allowedMacReadPort: AllowedMacReadPort = mock()
    private val applicationEventPublisher: ApplicationEventPublisher = mock()

    private val service =
        RouterAgentGrpcService(
            hub = hub,
            findAuthorizationTokenPort = findAuthorizationTokenPort,
            allowedMacReadPort = allowedMacReadPort,
            applicationEventPublisher = applicationEventPublisher,
        )

    @Test
    fun `authorized client observed is published as shared event`() {
        val responseObserver = mock<StreamObserver<RouterAgentCommand>>()
        val requestObserver = service.connect(responseObserver)

        requestObserver.onNext(
            RouterAgentMessage
                .newBuilder()
                .setAuthorizedClientObserved(
                    AuthorizedClientObserved
                        .newBuilder()
                        .setMacAddress("aa:bb:cc:dd:ee:ff")
                        .setObservedAt("2025-01-01T10:00:00Z")
                        .setHostname("office-laptop")
                        .setDhcpHostname("office-laptop.local")
                        .setDhcpVendorClass("android-dhcp-14")
                        .setDhcpPrlHash("hash-a"),
                ).build(),
        )

        val eventCaptor = argumentCaptor<AuthorizedClientObservedEvent>()
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture())

        val event = eventCaptor.firstValue
        assertEquals("aa:bb:cc:dd:ee:ff", event.macAddress)
        assertEquals("office-laptop", event.hostname)
        assertEquals("office-laptop.local", event.dhcpHostname)
        assertEquals("android-dhcp-14", event.dhcpVendorClass)
        assertEquals("hash-a", event.dhcpPrlHash)
    }

    @Test
    fun `authorized client observed keeps unset optional values as null`() {
        val responseObserver = mock<StreamObserver<RouterAgentCommand>>()
        val requestObserver = service.connect(responseObserver)

        requestObserver.onNext(
            RouterAgentMessage
                .newBuilder()
                .setAuthorizedClientObserved(
                    AuthorizedClientObserved
                        .newBuilder()
                        .setMacAddress("aa:bb:cc:dd:ee:ff"),
                ).build(),
        )

        val eventCaptor = argumentCaptor<AuthorizedClientObservedEvent>()
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture())

        val event = eventCaptor.lastValue
        assertEquals("aa:bb:cc:dd:ee:ff", event.macAddress)
        assertNull(event.hostname)
        assertNull(event.dhcpHostname)
        assertNull(event.dhcpVendorClass)
        assertNull(event.dhcpPrlHash)
    }
}
