package cz.grimir.wifimanager.captive.events.local

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.AuthorizedClientFingerprintGuard
import cz.grimir.wifimanager.captive.application.config.CaptiveFingerprintingProperties
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.UserAgentClassifier
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintProfile
import cz.grimir.wifimanager.shared.events.AuthorizedClientObservedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class AuthorizedClientObservedEventListenerTest {
    private val authorizedClientFingerprintGuard: AuthorizedClientFingerprintGuard = mock()
    private val deviceFingerprintService =
        DeviceFingerprintService(
            objectMapper = jacksonObjectMapper().findAndRegisterModules(),
            properties = CaptiveFingerprintingProperties(),
            userAgentClassifier = UserAgentClassifier { null },
        )

    private val listener =
        AuthorizedClientObservedEventListener(
            authorizedClientFingerprintGuard = authorizedClientFingerprintGuard,
            deviceFingerprintService = deviceFingerprintService,
        )

    @Test
    fun `event is converted to router fingerprint profile and forwarded to guard`() {
        listener.on(
            AuthorizedClientObservedEvent(
                macAddress = "aa:bb:cc:dd:ee:ff",
                hostname = "office-laptop",
                dhcpHostname = "office-laptop.local",
                dhcpVendorClass = "android-dhcp-14",
                dhcpPrlHash = "hash-a",
            ),
        )

        val profileCaptor = argumentCaptor<DeviceFingerprintProfile>()
        verify(authorizedClientFingerprintGuard).processAuthorizedClientObservation(
            mac = eq("aa:bb:cc:dd:ee:ff"),
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
