package cz.grimir.wifimanager.captive.application.authorization.support

import cz.grimir.wifimanager.captive.application.port.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.captive.application.query.model.AllowedMac
import cz.grimir.wifimanager.captive.application.query.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.support.ClientAccessAuthorizationResolver
import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import cz.grimir.wifimanager.captive.core.value.Device
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintStatus
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserId
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.time.Instant
import java.util.UUID

class ClientAccessAuthorizationResolverTest {
    private val allowedMacReadPort: AllowedMacReadPort = mock()
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort = mock()
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort = mock()
    private val timeProvider: TimeProvider = mock()
    private val mac = "aa:bb:cc:dd:ee:01"
    private val now = Instant.parse("2025-01-01T10:00:00Z")

    private val resolver =
        ClientAccessAuthorizationResolver(
            allowedMacReadPort = allowedMacReadPort,
            findAuthorizationTokenPort = findAuthorizationTokenPort,
            networkUserDeviceReadPort = networkUserDeviceReadPort,
            timeProvider = timeProvider,
        )

    @Test
    fun `active ticket device excludes kicked macs`() {
        given(findAuthorizationTokenPort.findByAuthorizedDeviceMac(mac)).willReturn(token(kicked = true))

        assertFalse(resolver.isAuthorizedByActiveTicketDevice(mac))
    }

    @Test
    fun `active ticket device excludes devices pending reauth`() {
        given(
            findAuthorizationTokenPort.findByAuthorizedDeviceMac(mac),
        ).willReturn(token(kicked = false, reauthRequiredAt = Instant.parse("2025-01-01T10:05:00Z")))

        assertFalse(resolver.isAuthorizedByActiveTicketDevice(mac))
    }

    @Test
    fun `active account device excludes devices pending reauth`() {
        given(networkUserDeviceReadPort.findByMac(mac)).willReturn(accountDevice(reauthRequiredAt = Instant.parse("2025-01-01T10:05:00Z")))

        assertFalse(resolver.isAuthorizedByAccountDevice(mac))
    }

    @Test
    fun `active account device returns false when no row exists`() {
        given(networkUserDeviceReadPort.findByMac(mac)).willReturn(null)

        assertFalse(resolver.isAuthorizedByAccountDevice(mac))
    }

    @Test
    fun `allowed mac excludes expired rows`() {
        given(timeProvider.get()).willReturn(now)
        given(allowedMacReadPort.findByMac(mac)).willReturn(AllowedMac(mac = mac, validUntil = now))

        assertFalse(resolver.isAuthorizedByAllowedMac(mac))
    }

    @Test
    fun `detects each active authorization source`() {
        given(allowedMacReadPort.findByMac(mac)).willReturn(AllowedMac(mac = mac, validUntil = now.plusSeconds(60)))
        given(timeProvider.get()).willReturn(now)
        given(findAuthorizationTokenPort.findByAuthorizedDeviceMac(mac)).willReturn(token(kicked = false))
        given(networkUserDeviceReadPort.findByMac(mac)).willReturn(accountDevice(reauthRequiredAt = null))

        assertTrue(resolver.isAuthorizedByAllowedMac(mac))
        assertTrue(resolver.isAuthorizedByActiveTicketDevice(mac))
        assertTrue(resolver.isAuthorizedByAccountDevice(mac))
    }

    @Test
    fun `privacy cleanup is not eligible for pending ticket reauth`() {
        given(findAuthorizationTokenPort.findByAuthorizedDeviceMac(mac)).willReturn(
            token(kicked = false, reauthRequiredAt = Instant.parse("2025-01-01T10:05:00Z")),
        )

        assertFalse(resolver.isDevicePrivacyCleanupEligible(mac))
    }

    @Test
    fun `privacy cleanup is not eligible for pending account reauth`() {
        given(networkUserDeviceReadPort.findByMac(mac)).willReturn(
            accountDevice(reauthRequiredAt = Instant.parse("2025-01-01T10:05:00Z")),
        )

        assertFalse(resolver.isDevicePrivacyCleanupEligible(mac))
    }

    @Test
    fun `privacy cleanup is eligible when ticket device was kicked and no other record remains`() {
        given(findAuthorizationTokenPort.findByAuthorizedDeviceMac(mac)).willReturn(token(kicked = true))

        assertTrue(resolver.isDevicePrivacyCleanupEligible(mac))
    }

    @Test
    fun `privacy cleanup is eligible when allowed mac is expired and no other record remains`() {
        given(timeProvider.get()).willReturn(now)
        given(allowedMacReadPort.findByMac(mac)).willReturn(AllowedMac(mac = mac, validUntil = now))

        assertTrue(resolver.isDevicePrivacyCleanupEligible(mac))
    }

    private fun token(
        kicked: Boolean,
        reauthRequiredAt: Instant? = null,
    ): AuthorizationToken =
        AuthorizationToken(
            id = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000401")),
            accessCode = "ABCDEFGH",
            validUntil = Instant.parse("2025-01-01T10:00:00Z"),
            requireUserNameOnLogin = false,
            authorizedDevices =
                mutableListOf(
                    Device(mac = mac, displayName = null, deviceName = null, reauthRequiredAt = reauthRequiredAt),
                ),
            kickedMacAddresses = if (kicked) mutableSetOf(mac) else mutableSetOf(),
        )

    private fun accountDevice(reauthRequiredAt: Instant?): NetworkUserDevice =
        NetworkUserDevice(
            userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000403")),
            mac = mac,
            name = "Work Laptop",
            hostname = "office-laptop",
            isRandomized = false,
            authorizedAt = Instant.parse("2025-01-01T09:00:00Z"),
            lastSeenAt = Instant.parse("2025-01-01T09:05:00Z"),
            fingerprintProfile = null,
            fingerprintStatus = DeviceFingerprintStatus.NONE,
            fingerprintVerifiedAt = null,
            reauthRequiredAt = reauthRequiredAt,
        )
}
