package cz.grimir.wifimanager.captive.routeragent.grpc

import cz.grimir.wifimanager.captive.application.command.ApplyAllowedClientsPresenceCommand
import cz.grimir.wifimanager.captive.application.command.handler.ApplyAllowedClientsPresenceUsecase
import cz.grimir.wifimanager.captive.application.port.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.shared.events.AuthorizedClientObservedEvent
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.context.ApplicationEventPublisher
import org.springframework.core.task.SyncTaskExecutor
import java.time.Instant

class RouterAgentGrpcServiceTest {
    private val hub: RouterAgentHub = mock()
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort = mock()
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort = mock()
    private val allowedMacReadPort: AllowedMacReadPort = mock()
    private val applicationEventPublisher: ApplicationEventPublisher = mock()
    private val applyAllowedClientsPresenceUsecase: ApplyAllowedClientsPresenceUsecase = mock()
    private val commandExecutor = SyncTaskExecutor()

    private val service =
        RouterAgentGrpcService(
            hub = hub,
            findAuthorizationTokenPort = findAuthorizationTokenPort,
            networkUserDeviceReadPort = networkUserDeviceReadPort,
            allowedMacReadPort = allowedMacReadPort,
            applicationEventPublisher = applicationEventPublisher,
            applyAllowedClientsPresenceUsecase = applyAllowedClientsPresenceUsecase,
            commandExecutor = commandExecutor,
        )

    @Test
    fun `synchronize pushes ticket devices account devices and allowed macs`() {
        given(findAuthorizationTokenPort.findAllAuthorizedMacs()).willReturn(listOf("AA:AA:AA:AA:AA:AA", "SHARED"))
        given(networkUserDeviceReadPort.findAllAuthorizedMacs()).willReturn(listOf("BB:BB:BB:BB:BB:BB", "SHARED"))
        given(allowedMacReadPort.findAllAuthorizedMacs()).willReturn(listOf("CC:CC:CC:CC:CC:CC"))

        val responseObserver = mock<StreamObserver<RouterAgentCommand>>()
        val requestObserver = service.connect(responseObserver)

        requestObserver.onNext(
            RouterAgentMessage
                .newBuilder()
                .setSynchronize(Synchronize.newBuilder().build())
                .build(),
        )

        verify(hub).broadcastSetAllowedClients(
            eq(listOf("AA:AA:AA:AA:AA:AA", "SHARED", "BB:BB:BB:BB:BB:BB", "CC:CC:CC:CC:CC:CC")),
        )
    }

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

    @Test
    fun `allowed clients presence invokes apply usecase`() {
        val responseObserver = mock<StreamObserver<RouterAgentCommand>>()
        val requestObserver = service.connect(responseObserver)

        requestObserver.onNext(
            RouterAgentMessage
                .newBuilder()
                .setAllowedClientsPresence(
                    AllowedClientsPresence
                        .newBuilder()
                        .setReportAt("2025-03-01T12:00:00Z")
                        .addEntries(
                            cz.grimir.wifimanager.captive.routeragent.grpc.AllowedClientsPresenceEntry
                                .newBuilder()
                                .setMacAddress("aa:bb:cc:dd:ee:ff")
                                .setLastSeenAt("2025-03-01T11:30:00Z")
                                .setNeighborStatus(NeighborStatus.NEIGHBOR_STATUS_STALE)
                                .build(),
                        ).build(),
                ).build(),
        )

        val cmdCaptor = argumentCaptor<ApplyAllowedClientsPresenceCommand>()
        verify(applyAllowedClientsPresenceUsecase).apply(cmdCaptor.capture())
        val cmd = cmdCaptor.firstValue
        assertEquals(1, cmd.entries.size)
        assertEquals("aa:bb:cc:dd:ee:ff", cmd.entries[0].macAddress)
        assertEquals(Instant.parse("2025-03-01T11:30:00Z"), cmd.entries[0].lastSeenAt)
    }
}
