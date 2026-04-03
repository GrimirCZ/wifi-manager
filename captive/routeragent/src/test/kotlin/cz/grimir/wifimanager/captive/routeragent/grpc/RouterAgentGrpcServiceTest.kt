package cz.grimir.wifimanager.captive.routeragent.grpc

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import cz.grimir.wifimanager.captive.application.allowedmac.port.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.authorization.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.devicefingerprint.AuthorizedClientFingerprintGuard
import cz.grimir.wifimanager.captive.application.devicefingerprint.CaptiveFingerprintingProperties
import cz.grimir.wifimanager.captive.application.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintProfile
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.core.task.SyncTaskExecutor

class RouterAgentGrpcServiceTest {
    private val hub: RouterAgentHub = mock()
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort = mock()
    private val allowedMacReadPort: AllowedMacReadPort = mock()
    private val authorizedClientFingerprintGuard: AuthorizedClientFingerprintGuard = mock()
    private val deviceFingerprintService =
        DeviceFingerprintService(
            objectMapper = jacksonObjectMapper().findAndRegisterModules(),
            properties = CaptiveFingerprintingProperties(),
            userAgentClassifier = cz.grimir.wifimanager.captive.application.devicefingerprint.UserAgentClassifier { null },
        )

    private val service =
        RouterAgentGrpcService(
            hub = hub,
            findAuthorizationTokenPort = findAuthorizationTokenPort,
            allowedMacReadPort = allowedMacReadPort,
            authorizedClientFingerprintGuard = authorizedClientFingerprintGuard,
            deviceFingerprintService = deviceFingerprintService,
            commandExecutor = SyncTaskExecutor(),
        )

    @Test
    fun `authorized client observed is converted to router observation and forwarded to guard`() {
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

        val profileCaptor = argumentCaptor<DeviceFingerprintProfile>()
        verify(authorizedClientFingerprintGuard).processAuthorizedClientObservation(
            mac = org.mockito.kotlin.eq("aa:bb:cc:dd:ee:ff"),
            currentFingerprint = profileCaptor.capture(),
        )

        val profile = profileCaptor.firstValue
        assertNotNull(profile)
        assertEquals("hash-a", profile.signals[DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH]?.value)
        assertEquals("android-dhcp-14", profile.signals[DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS]?.value)
        assertEquals("office-laptop.local", profile.signals[DeviceFingerprintService.SIGNAL_EXACT_HOSTNAME]?.value)
        assertEquals("office-laptop", profile.signals[DeviceFingerprintService.SIGNAL_HOSTNAME_STEM]?.value)
    }
}
