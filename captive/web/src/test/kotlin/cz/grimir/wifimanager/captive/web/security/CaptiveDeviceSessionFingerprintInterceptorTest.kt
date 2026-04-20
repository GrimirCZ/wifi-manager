package cz.grimir.wifimanager.captive.web.security

import cz.grimir.wifimanager.captive.application.devicefingerprint.AuthorizedClientFingerprintGuard
import cz.grimir.wifimanager.captive.application.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintProfile
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintSignal
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintSignalStrength
import cz.grimir.wifimanager.captive.web.security.support.ClientInfo
import cz.grimir.wifimanager.captive.web.security.support.CurrentClientResolver
import cz.grimir.wifimanager.shared.security.mvc.ClientIdentityUnavailableException
import cz.grimir.wifimanager.shared.security.mvc.MissingClientMacException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.slf4j.MDC

class CaptiveDeviceSessionFingerprintInterceptorTest {
    private val currentClientResolver: CurrentClientResolver = mock()
    private val authorizedClientFingerprintGuard: AuthorizedClientFingerprintGuard = mock()
    private val interceptor =
        CaptiveDeviceSessionFingerprintInterceptor(
            currentClientResolver = currentClientResolver,
            authorizedClientFingerprintGuard = authorizedClientFingerprintGuard,
        )

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun `matching request continues and processes guard`() {
        val request = mock<HttpServletRequest>()
        val response = mock<HttpServletResponse>()
        val current = matchingClientInfo()
        given(currentClientResolver.resolve(request)).willReturn(current)

        val allowed = interceptor.preHandle(request, response, Any())

        assertTrue(allowed)
        org.junit.jupiter.api.Assertions.assertEquals("aa:bb:cc:dd:ee:ff", MDC.get("clientMac"))
        verify(authorizedClientFingerprintGuard).refreshAuthenticatedClientFingerprint(current.macAddress, current.fingerprintProfile)
        verifyNoInteractions(response)
    }

    @Test
    fun `changed request fingerprint still continues and processes guard`() {
        val request = mock<HttpServletRequest>()
        val response = mock<HttpServletResponse>()
        val current = breachedClientInfo()
        given(currentClientResolver.resolve(request)).willReturn(current)

        val allowed = interceptor.preHandle(request, response, Any())

        assertTrue(allowed)
        verify(authorizedClientFingerprintGuard).refreshAuthenticatedClientFingerprint(current.macAddress, current.fingerprintProfile)
        verifyNoInteractions(response)
    }

    @Test
    fun `missing client identity continues without refreshing fingerprint`() {
        val request = mock<HttpServletRequest>()
        val response = mock<HttpServletResponse>()
        given(currentClientResolver.resolve(request)).willThrow(ClientIdentityUnavailableException("222"))

        val allowed = interceptor.preHandle(request, response, Any())

        assertTrue(allowed)
        verifyNoInteractions(authorizedClientFingerprintGuard, response)
    }

    @Test
    fun `blank client mac continues without refreshing fingerprint`() {
        val request = mock<HttpServletRequest>()
        val response = mock<HttpServletResponse>()
        given(currentClientResolver.resolve(request)).willThrow(MissingClientMacException("111"))

        val allowed = interceptor.preHandle(request, response, Any())

        assertTrue(allowed)
        verifyNoInteractions(authorizedClientFingerprintGuard, response)
    }

    private fun matchingClientInfo(): ClientInfo =
        ClientInfo(
            ipAddress = "192.168.0.10",
            macAddress = "aa:bb:cc:dd:ee:ff",
            hostname = "office-laptop",
            dhcpVendorClass = "android-dhcp-14",
            dhcpPrlHash = "hash-a",
            dhcpHostname = "office-laptop",
            fingerprintProfile = acceptedFingerprint(),
        )

    private fun breachedClientInfo(): ClientInfo =
        matchingClientInfo().copy(
            fingerprintProfile =
                DeviceFingerprintProfile(
                    signals =
                        linkedMapOf(
                            DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to signal("hash-b", DeviceFingerprintSignalStrength.STRONG),
                            DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS to signal("android-dhcp-15", DeviceFingerprintSignalStrength.MEDIUM),
                        ),
                ),
        )

    private fun acceptedFingerprint(): DeviceFingerprintProfile =
        DeviceFingerprintProfile(
            signals =
                linkedMapOf(
                    DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to signal("hash-a", DeviceFingerprintSignalStrength.STRONG),
                    DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS to signal("android-dhcp-14", DeviceFingerprintSignalStrength.MEDIUM),
                ),
        )

    private fun signal(
        value: String,
        strength: DeviceFingerprintSignalStrength,
    ) = DeviceFingerprintSignal(value = value, source = "router-agent:dhcp-log", strength = strength)
}
