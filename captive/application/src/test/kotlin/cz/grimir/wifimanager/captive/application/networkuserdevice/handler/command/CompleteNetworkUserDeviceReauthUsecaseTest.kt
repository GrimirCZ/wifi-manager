package cz.grimir.wifimanager.captive.application.networkuserdevice.handler.command

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import cz.grimir.wifimanager.captive.application.command.handler.CompleteNetworkUserDeviceReauthUsecase
import cz.grimir.wifimanager.captive.application.config.CaptiveFingerprintingProperties
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.UserAgentClassifier
import cz.grimir.wifimanager.captive.application.query.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintProfile
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintSignal
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintSignalStrength
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import java.time.Instant
import java.util.UUID

class CompleteNetworkUserDeviceReauthUsecaseTest {
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort = mock()
    private val networkUserDeviceWritePort: NetworkUserDeviceWritePort = mock()
    private val timeProvider: TimeProvider = mock()
    private val now = Instant.parse("2025-01-01T10:15:00Z")
    private val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000111"))

    private val deviceFingerprintService =
        DeviceFingerprintService(
            objectMapper = jacksonObjectMapper().findAndRegisterModules(),
            properties = CaptiveFingerprintingProperties(),
            userAgentClassifier = UserAgentClassifier { null },
        )

    private val usecase =
        CompleteNetworkUserDeviceReauthUsecase(
            networkUserDeviceReadPort = networkUserDeviceReadPort,
            networkUserDeviceWritePort = networkUserDeviceWritePort,
            deviceFingerprintService = deviceFingerprintService,
            timeProvider = timeProvider,
        )

    @Test
    fun `complete reauth replaces accepted fingerprint and clears reauth`() {
        val existing =
            accountDevice(
                fingerprintProfile =
                    profile(
                        DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to signal("hash-a", DeviceFingerprintSignalStrength.STRONG),
                        DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS to
                            signal("android-dhcp-14", DeviceFingerprintSignalStrength.MEDIUM),
                        DeviceFingerprintService.SIGNAL_TLS to signal("tls-a", DeviceFingerprintSignalStrength.STRONG),
                    ),
                reauthRequiredAt = Instant.parse("2025-01-01T10:10:00Z"),
            )
        val current =
            profile(
                DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to signal("hash-b", DeviceFingerprintSignalStrength.STRONG),
                DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS to signal("android-dhcp-15", DeviceFingerprintSignalStrength.MEDIUM),
            )
        given(networkUserDeviceReadPort.findByMac(existing.mac)).willReturn(existing)
        given(timeProvider.get()).willReturn(now)

        val updated = usecase.complete(userId = userId, mac = existing.mac, currentFingerprint = current)

        assertEquals(now, updated?.fingerprintVerifiedAt)
        assertEquals(null, updated?.reauthRequiredAt)
        assertEquals(
            "hash-b",
            updated
                ?.fingerprintProfile
                ?.signals
                ?.get(DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH)
                ?.value,
        )
        assertEquals(
            "android-dhcp-15",
            updated
                ?.fingerprintProfile
                ?.signals
                ?.get(DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS)
                ?.value,
        )
        assertEquals(null, updated?.fingerprintProfile?.signals?.get(DeviceFingerprintService.SIGNAL_TLS))

        val savedCaptor = argumentCaptor<NetworkUserDevice>()
        verify(networkUserDeviceWritePort).save(savedCaptor.capture())
        assertEquals(updated, savedCaptor.firstValue)
    }

    private fun accountDevice(
        fingerprintProfile: DeviceFingerprintProfile?,
        reauthRequiredAt: Instant?,
    ): NetworkUserDevice =
        NetworkUserDevice(
            userId = userId,
            mac = "aa:bb:cc:dd:ee:01",
            name = "Work Laptop",
            hostname = "office-laptop",
            isRandomized = false,
            authorizedAt = Instant.parse("2025-01-01T10:00:00Z"),
            lastSeenAt = Instant.parse("2025-01-01T10:10:00Z"),
            fingerprintProfile = fingerprintProfile,
            fingerprintStatus = deviceFingerprintService.status(fingerprintProfile),
            fingerprintVerifiedAt = Instant.parse("2025-01-01T10:00:00Z"),
            reauthRequiredAt = reauthRequiredAt,
        )

    private fun profile(vararg entries: Pair<String, DeviceFingerprintSignal>): DeviceFingerprintProfile =
        DeviceFingerprintProfile(signals = linkedMapOf(*entries))

    private fun signal(
        value: String,
        strength: DeviceFingerprintSignalStrength,
    ) = DeviceFingerprintSignal(value = value, source = "router-agent:test", strength = strength)
}
