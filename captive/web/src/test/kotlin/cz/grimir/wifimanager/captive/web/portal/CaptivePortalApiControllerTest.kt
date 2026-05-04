package cz.grimir.wifimanager.captive.web.portal

import cz.grimir.wifimanager.captive.application.port.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.query.model.AllowedMac
import cz.grimir.wifimanager.captive.application.query.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.AuthorizedClientFingerprintGuard
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.AuthorizedMacState
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.AuthorizedMacVerification
import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import cz.grimir.wifimanager.captive.core.value.Device
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintStatus
import cz.grimir.wifimanager.captive.web.security.support.ClientInfo
import cz.grimir.wifimanager.shared.application.captive.CaptivePortalApiProperties
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.time.Instant
import java.util.UUID

class CaptivePortalApiControllerTest {
    private val authorizedClientFingerprintGuard: AuthorizedClientFingerprintGuard = mock()
    private val allowedMacReadPort: AllowedMacReadPort = mock()
    private val timeProvider: TimeProvider = mock()

    private val service =
        CaptiveClientAccessStatusService(
            authorizedClientFingerprintGuard = authorizedClientFingerprintGuard,
            allowedMacReadPort = allowedMacReadPort,
            timeProvider = timeProvider,
        )

    private val controller =
        CaptivePortalApiController(
            clientAccessStatusService = service,
            properties = CaptivePortalApiProperties(publicBaseUrl = "https://portal.example"),
        )

    private val now = Instant.parse("2026-04-10T10:00:00Z")

    private val clientInfo =
        ClientInfo(
            ipAddress = "192.168.0.10",
            macAddress = "AA:BB:CC:DD:EE:FF",
            hostname = "guest-phone",
            dhcpVendorClass = null,
            dhcpPrlHash = null,
            dhcpHostname = null,
            fingerprintProfile = null,
        )

    @Test
    fun `unauthorized client returns captive true with portal url`() {
        given(timeProvider.get()).willReturn(now)
        given(authorizedClientFingerprintGuard.verifyAuthorizedMac(clientInfo.macAddress, clientInfo.fingerprintProfile))
            .willReturn(AuthorizedMacVerification(AuthorizedMacState.NONE))
        given(allowedMacReadPort.findByMac(clientInfo.macAddress)).willReturn(null)

        val response = controller.api(clientInfo)

        assertEquals(200, response.statusCode.value())
        assertEquals("application/captive+json", response.headers.contentType.toString())
        assertTrue(response.headers.cacheControl!!.contains("private"))
        assertTrue(response.body!!.captive)
        assertEquals("https://portal.example/captive", response.body!!.userPortalUrl)
        assertNull(response.body!!.secondsRemaining)
    }

    @Test
    fun `active account device returns captive false without remaining seconds`() {
        given(timeProvider.get()).willReturn(now)
        given(authorizedClientFingerprintGuard.verifyAuthorizedMac(clientInfo.macAddress, clientInfo.fingerprintProfile))
            .willReturn(
                AuthorizedMacVerification(
                    state = AuthorizedMacState.ACTIVE_NETWORK_USER_DEVICE,
                    networkUserDevice =
                        NetworkUserDevice(
                            userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000111")),
                            mac = clientInfo.macAddress,
                            name = "Work Laptop",
                            hostname = clientInfo.hostname,
                            isRandomized = false,
                            authorizedAt = now.minusSeconds(600),
                            lastSeenAt = now,
                            fingerprintProfile = null,
                            fingerprintStatus = DeviceFingerprintStatus.NONE,
                            fingerprintVerifiedAt = null,
                            reauthRequiredAt = null,
                        ),
                ),
            )

        val response = controller.api(clientInfo)

        assertFalse(response.body!!.captive)
        assertNull(response.body!!.secondsRemaining)
    }

    @Test
    fun `active ticket device returns captive false with remaining seconds`() {
        val token = ticketToken(validUntil = now.plusSeconds(90))
        given(timeProvider.get()).willReturn(now)
        given(authorizedClientFingerprintGuard.verifyAuthorizedMac(clientInfo.macAddress, clientInfo.fingerprintProfile))
            .willReturn(
                AuthorizedMacVerification(
                    state = AuthorizedMacState.ACTIVE_TICKET_DEVICE,
                    token = token,
                    ticketDevice = token.authorizedDevices.single(),
                ),
            )

        val response = controller.api(clientInfo)

        assertFalse(response.body!!.captive)
        assertEquals(90, response.body!!.secondsRemaining)
    }

    @Test
    fun `ticket remaining seconds are omitted when token expires now`() {
        val token = ticketToken(validUntil = now)
        given(timeProvider.get()).willReturn(now)
        given(authorizedClientFingerprintGuard.verifyAuthorizedMac(clientInfo.macAddress, clientInfo.fingerprintProfile))
            .willReturn(
                AuthorizedMacVerification(
                    state = AuthorizedMacState.ACTIVE_TICKET_DEVICE,
                    token = token,
                    ticketDevice = token.authorizedDevices.single(),
                ),
            )
        given(allowedMacReadPort.findByMac(clientInfo.macAddress)).willReturn(null)

        val response = controller.api(clientInfo)

        assertTrue(response.body!!.captive)
        assertNull(response.body!!.secondsRemaining)
    }

    @Test
    fun `active allowed mac without expiry returns captive false without remaining seconds`() {
        given(timeProvider.get()).willReturn(now)
        given(authorizedClientFingerprintGuard.verifyAuthorizedMac(clientInfo.macAddress, clientInfo.fingerprintProfile))
            .willReturn(AuthorizedMacVerification(AuthorizedMacState.NONE))
        given(allowedMacReadPort.findByMac(clientInfo.macAddress)).willReturn(AllowedMac(clientInfo.macAddress, null))

        val response = controller.api(clientInfo)

        assertFalse(response.body!!.captive)
        assertNull(response.body!!.secondsRemaining)
    }

    @Test
    fun `active allowed mac with future expiry returns captive false with remaining seconds`() {
        given(timeProvider.get()).willReturn(now)
        given(authorizedClientFingerprintGuard.verifyAuthorizedMac(clientInfo.macAddress, clientInfo.fingerprintProfile))
            .willReturn(AuthorizedMacVerification(AuthorizedMacState.NONE))
        given(allowedMacReadPort.findByMac(clientInfo.macAddress))
            .willReturn(AllowedMac(clientInfo.macAddress, now.plusSeconds(120)))

        val response = controller.api(clientInfo)

        assertFalse(response.body!!.captive)
        assertEquals(120, response.body!!.secondsRemaining)
    }

    @Test
    fun `expired allowed mac returns captive true without remaining seconds`() {
        given(timeProvider.get()).willReturn(now)
        given(authorizedClientFingerprintGuard.verifyAuthorizedMac(clientInfo.macAddress, clientInfo.fingerprintProfile))
            .willReturn(AuthorizedMacVerification(AuthorizedMacState.NONE))
        given(allowedMacReadPort.findByMac(clientInfo.macAddress))
            .willReturn(AllowedMac(clientInfo.macAddress, now.minusSeconds(1)))

        val response = controller.api(clientInfo)

        assertTrue(response.body!!.captive)
        assertNull(response.body!!.secondsRemaining)
    }

    @Test
    fun `kicked ticket device returns captive true without remaining seconds`() {
        val token = ticketToken(validUntil = now.plusSeconds(600), kicked = true)
        given(timeProvider.get()).willReturn(now)
        given(authorizedClientFingerprintGuard.verifyAuthorizedMac(clientInfo.macAddress, clientInfo.fingerprintProfile))
            .willReturn(
                AuthorizedMacVerification(
                    state = AuthorizedMacState.ACTIVE_TICKET_DEVICE,
                    token = token,
                    ticketDevice = token.authorizedDevices.single(),
                ),
            )

        val response = controller.api(clientInfo)

        assertTrue(response.body!!.captive)
        assertNull(response.body!!.secondsRemaining)
    }

    @Test
    fun `reauth required ticket device returns captive true without remaining seconds`() {
        val token = ticketToken(validUntil = now.plusSeconds(600))
        val ticketDevice = token.authorizedDevices.single().copy(reauthRequiredAt = now.minusSeconds(5))
        given(timeProvider.get()).willReturn(now)
        given(authorizedClientFingerprintGuard.verifyAuthorizedMac(clientInfo.macAddress, clientInfo.fingerprintProfile))
            .willReturn(
                AuthorizedMacVerification(
                    state = AuthorizedMacState.REAUTH_REQUIRED,
                    token = token,
                    ticketDevice = ticketDevice,
                ),
            )

        val response = controller.api(clientInfo)

        assertTrue(response.body!!.captive)
        assertNull(response.body!!.secondsRemaining)
    }

    @Test
    fun `reauth required account device returns captive true without remaining seconds`() {
        given(timeProvider.get()).willReturn(now)
        given(authorizedClientFingerprintGuard.verifyAuthorizedMac(clientInfo.macAddress, clientInfo.fingerprintProfile))
            .willReturn(
                AuthorizedMacVerification(
                    state = AuthorizedMacState.REAUTH_REQUIRED,
                    networkUserDevice =
                        NetworkUserDevice(
                            userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000111")),
                            mac = clientInfo.macAddress,
                            name = "Work Laptop",
                            hostname = clientInfo.hostname,
                            isRandomized = false,
                            authorizedAt = now.minusSeconds(600),
                            lastSeenAt = now,
                            fingerprintProfile = null,
                            fingerprintStatus = DeviceFingerprintStatus.NONE,
                            fingerprintVerifiedAt = null,
                            reauthRequiredAt = now.minusSeconds(10),
                        ),
                ),
            )

        val response = controller.api(clientInfo)

        assertTrue(response.body!!.captive)
        assertNull(response.body!!.secondsRemaining)
    }

    private fun ticketToken(
        validUntil: Instant,
        kicked: Boolean = false,
    ): AuthorizationToken {
        val device =
            Device(
                mac = clientInfo.macAddress,
                displayName = "Guest User",
                deviceName = clientInfo.hostname,
                fingerprintProfile = null,
            )

        return AuthorizationToken(
            id = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000300")),
            accessCode = "ABCDEF",
            validUntil = validUntil,
            requireUserNameOnLogin = false,
            authorizedDevices = mutableListOf(device),
            kickedMacAddresses = if (kicked) mutableSetOf(clientInfo.macAddress) else mutableSetOf(),
        )
    }
}
