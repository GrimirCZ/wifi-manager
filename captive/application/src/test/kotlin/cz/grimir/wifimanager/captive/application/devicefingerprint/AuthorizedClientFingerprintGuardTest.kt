package cz.grimir.wifimanager.captive.application.devicefingerprint

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import cz.grimir.wifimanager.captive.application.event.MacAuthorizationStateChangedEvent
import cz.grimir.wifimanager.captive.application.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.port.ModifyAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.config.CaptiveFingerprintingProperties
import cz.grimir.wifimanager.captive.application.query.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.captive.application.port.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.AuthorizedClientFingerprintGuard
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.AuthorizedMacState
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.UserAgentClassifier
import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import cz.grimir.wifimanager.captive.core.value.Device
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintProfile
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintSignal
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintSignalStrength
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.events.DeviceFingerprintActionTaken
import cz.grimir.wifimanager.shared.events.DeviceFingerprintMismatchDetectedEvent
import cz.grimir.wifimanager.shared.events.DeviceFingerprintSubjectType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.time.Instant
import java.util.UUID

class AuthorizedClientFingerprintGuardTest {
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort = mock()
    private val networkUserDeviceWritePort: NetworkUserDeviceWritePort = mock()
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort = mock()
    private val modifyAuthorizationTokenPort: ModifyAuthorizationTokenPort = mock()
    private val eventPublisher: CaptiveEventPublisher = mock()
    private val timeProvider: TimeProvider = mock()
    private val now = Instant.parse("2025-01-01T10:15:00Z")

    private val deviceFingerprintService =
        DeviceFingerprintService(
            objectMapper = jacksonObjectMapper().findAndRegisterModules(),
            properties = CaptiveFingerprintingProperties(),
            userAgentClassifier = UserAgentClassifier { null },
        )

    private val guard =
        AuthorizedClientFingerprintGuard(
            networkUserDeviceReadPort = networkUserDeviceReadPort,
            networkUserDeviceWritePort = networkUserDeviceWritePort,
            findAuthorizationTokenPort = findAuthorizationTokenPort,
            modifyAuthorizationTokenPort = modifyAuthorizationTokenPort,
            eventPublisher = eventPublisher,
            timeProvider = timeProvider,
            deviceFingerprintService = deviceFingerprintService,
        )

    @Test
    fun `matching account device stays active without side effects`() {
        val device = accountDevice(fingerprintProfile = acceptedEnforceableFingerprint())
        given(timeProvider.get()).willReturn(now)
        given(networkUserDeviceReadPort.findByMac(device.mac)).willReturn(device)

        val result = guard.verifyAuthorizedMac(device.mac, matchingFingerprint())

        assertEquals(AuthorizedMacState.ACTIVE_NETWORK_USER_DEVICE, result.state)
        assertNull(result.networkUserDevice?.reauthRequiredAt)
        verifyNoInteractions(networkUserDeviceWritePort, eventPublisher, findAuthorizationTokenPort, modifyAuthorizationTokenPort)
    }

    @Test
    fun `matching ticket device stays active without side effects`() {
        val token = ticketToken(ticketDevice(fingerprintProfile = acceptedEnforceableFingerprint()))
        given(timeProvider.get()).willReturn(now)
        given(networkUserDeviceReadPort.findByMac(ticketMac())).willReturn(null)
        given(findAuthorizationTokenPort.findByAuthorizedDeviceMac(ticketMac())).willReturn(token)

        val result = guard.verifyAuthorizedMac(ticketMac(), matchingFingerprint())

        assertEquals(AuthorizedMacState.ACTIVE_TICKET_DEVICE, result.state)
        assertNull(result.ticketDevice?.reauthRequiredAt)
        verifyNoInteractions(networkUserDeviceWritePort, eventPublisher)
        verify(modifyAuthorizationTokenPort, never()).save(token)
    }

    @Test
    fun `alarm only mismatch logs without revoking account device`() {
        val device = accountDevice(fingerprintProfile = acceptedEnforceableFingerprint())
        given(timeProvider.get()).willReturn(now)
        given(networkUserDeviceReadPort.findByMac(device.mac)).willReturn(device)

        val result = guard.verifyAuthorizedMac(device.mac, alarmOnlyFingerprint())

        assertEquals(AuthorizedMacState.ACTIVE_NETWORK_USER_DEVICE, result.state)
        assertNull(result.networkUserDevice?.reauthRequiredAt)
        verify(networkUserDeviceWritePort, never()).save(org.mockito.kotlin.any())

        val eventCaptor = argumentCaptor<DeviceFingerprintMismatchDetectedEvent>()
        verify(eventPublisher).publish(eventCaptor.capture())
        assertEquals(false, eventCaptor.firstValue.breached)
        assertEquals(DeviceFingerprintActionTaken.LOG_ONLY, eventCaptor.firstValue.actionTaken)
        assertEquals(DeviceFingerprintSubjectType.NETWORK_USER_DEVICE, eventCaptor.firstValue.subjectType)
        assertEquals(deviceFingerprintService.toJson(device.fingerprintProfile), eventCaptor.firstValue.previousFingerprint)
        assertEquals(deviceFingerprintService.toJson(alarmOnlyFingerprint()), eventCaptor.firstValue.currentFingerprint)
        assertEquals(deviceFingerprintService.sourcesToJson(device.fingerprintProfile), eventCaptor.firstValue.previousSources)
        assertEquals(deviceFingerprintService.sourcesToJson(alarmOnlyFingerprint()), eventCaptor.firstValue.currentSources)
    }

    @Test
    fun `breached account device sets reauth required and revokes access`() {
        val device = accountDevice(fingerprintProfile = acceptedEnforceableFingerprint())
        given(timeProvider.get()).willReturn(now)
        given(networkUserDeviceReadPort.findByMac(device.mac)).willReturn(device)

        val result = guard.verifyAuthorizedMac(device.mac, breachedFingerprint())

        assertEquals(AuthorizedMacState.REAUTH_REQUIRED, result.state)
        assertEquals(now, result.networkUserDevice?.reauthRequiredAt)
        val savedCaptor = argumentCaptor<NetworkUserDevice>()
        verify(networkUserDeviceWritePort).save(savedCaptor.capture())
        assertEquals(now, savedCaptor.firstValue.reauthRequiredAt)
        verify(eventPublisher).publish(MacAuthorizationStateChangedEvent(listOf(device.mac)))

        val eventCaptor = argumentCaptor<DeviceFingerprintMismatchDetectedEvent>()
        verify(eventPublisher).publish(eventCaptor.capture())
        assertEquals(true, eventCaptor.firstValue.breached)
        assertEquals(DeviceFingerprintActionTaken.REAUTH_REQUIRED, eventCaptor.firstValue.actionTaken)
    }

    @Test
    fun `breached ticket device sets reauth required and revokes access`() {
        val token = ticketToken(ticketDevice(fingerprintProfile = acceptedEnforceableFingerprint()))
        given(timeProvider.get()).willReturn(now)
        given(networkUserDeviceReadPort.findByMac(ticketMac())).willReturn(null)
        given(findAuthorizationTokenPort.findByAuthorizedDeviceMac(ticketMac())).willReturn(token)

        val result = guard.verifyAuthorizedMac(ticketMac(), breachedFingerprint())

        assertEquals(AuthorizedMacState.REAUTH_REQUIRED, result.state)
        assertEquals(now, result.ticketDevice?.reauthRequiredAt)
        verify(modifyAuthorizationTokenPort).save(token)
        verify(eventPublisher).publish(MacAuthorizationStateChangedEvent(listOf(ticketMac())))

        val eventCaptor = argumentCaptor<DeviceFingerprintMismatchDetectedEvent>()
        verify(eventPublisher).publish(eventCaptor.capture())
        assertEquals(DeviceFingerprintSubjectType.TICKET_DEVICE, eventCaptor.firstValue.subjectType)
        assertEquals(token.id, eventCaptor.firstValue.ticketId)
    }

    @Test
    fun `matching observation enriches only missing account signals`() {
        val accepted =
            profile(
                DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to
                    signal("hash-a", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.STRONG),
                DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS to
                    signal("android-dhcp-14", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.MEDIUM),
            )
        val current =
            profile(
                DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to
                    signal("hash-a", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.STRONG),
                DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS to
                    signal("android-dhcp-14", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.MEDIUM),
                DeviceFingerprintService.SIGNAL_HOSTNAME_STEM to
                    signal("office-laptop", "router-agent:hostname", DeviceFingerprintSignalStrength.WEAK),
            )
        val device = accountDevice(fingerprintProfile = accepted)
        given(timeProvider.get()).willReturn(now)
        given(networkUserDeviceReadPort.findByMac(device.mac)).willReturn(device)

        val result = guard.verifyAuthorizedMac(device.mac, current)

        assertEquals(AuthorizedMacState.ACTIVE_NETWORK_USER_DEVICE, result.state)
        val savedCaptor = argumentCaptor<NetworkUserDevice>()
        verify(networkUserDeviceWritePort).save(savedCaptor.capture())
        assertEquals(
            "hash-a",
            savedCaptor.firstValue.fingerprintProfile
                ?.signals
                ?.get(DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH)
                ?.value,
        )
        assertEquals(
            "office-laptop",
            savedCaptor.firstValue.fingerprintProfile
                ?.signals
                ?.get(DeviceFingerprintService.SIGNAL_HOSTNAME_STEM)
                ?.value,
        )
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `authenticated session breach marks account device for reauth without refreshing accepted fingerprint`() {
        val device = accountDevice(fingerprintProfile = acceptedEnforceableFingerprint())
        given(timeProvider.get()).willReturn(now)
        given(networkUserDeviceReadPort.findByMac(device.mac)).willReturn(device)

        guard.refreshAuthenticatedClientFingerprint(device.mac, breachedFingerprint())

        val savedCaptor = argumentCaptor<NetworkUserDevice>()
        verify(networkUserDeviceWritePort).save(savedCaptor.capture())
        assertEquals(now, savedCaptor.firstValue.reauthRequiredAt)
        assertEquals(
            "hash-a",
            savedCaptor.firstValue.fingerprintProfile
                ?.signals
                ?.get(DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH)
                ?.value,
        )
        assertEquals(
            "android-dhcp-14",
            savedCaptor.firstValue.fingerprintProfile
                ?.signals
                ?.get(DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS)
                ?.value,
        )
        assertEquals(
            "office-laptop",
            savedCaptor.firstValue.fingerprintProfile
                ?.signals
                ?.get(DeviceFingerprintService.SIGNAL_HOSTNAME_STEM)
                ?.value,
        )
        assertEquals(device.fingerprintVerifiedAt, savedCaptor.firstValue.fingerprintVerifiedAt)
        verify(eventPublisher).publish(MacAuthorizationStateChangedEvent(listOf(device.mac)))
        val eventCaptor = argumentCaptor<DeviceFingerprintMismatchDetectedEvent>()
        verify(eventPublisher).publish(eventCaptor.capture())
        assertEquals(true, eventCaptor.firstValue.breached)
        verifyNoInteractions(findAuthorizationTokenPort, modifyAuthorizationTokenPort)
    }

    @Test
    fun `authenticated session active account device refreshes observed signals after verification`() {
        val device = accountDevice(fingerprintProfile = acceptedEnforceableFingerprint())
        given(timeProvider.get()).willReturn(now)
        given(networkUserDeviceReadPort.findByMac(device.mac)).willReturn(device)

        guard.refreshAuthenticatedClientFingerprint(device.mac, activeRefreshFingerprint())

        val savedCaptor = argumentCaptor<NetworkUserDevice>()
        verify(networkUserDeviceWritePort).save(savedCaptor.capture())
        assertNull(savedCaptor.firstValue.reauthRequiredAt)
        assertEquals(
            "hash-a",
            savedCaptor.firstValue.fingerprintProfile
                ?.signals
                ?.get(DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH)
                ?.value,
        )
        assertEquals(
            "field-laptop",
            savedCaptor.firstValue.fingerprintProfile
                ?.signals
                ?.get(DeviceFingerprintService.SIGNAL_HOSTNAME_STEM)
                ?.value,
        )
        assertEquals(now, savedCaptor.firstValue.fingerprintVerifiedAt)
        verifyNoInteractions(eventPublisher, findAuthorizationTokenPort, modifyAuthorizationTokenPort)
    }

    @Test
    fun `authenticated session breach marks ticket device for reauth without refreshing accepted fingerprint`() {
        val token = ticketToken(ticketDevice(fingerprintProfile = acceptedEnforceableFingerprint()))
        given(timeProvider.get()).willReturn(now)
        given(networkUserDeviceReadPort.findByMac(ticketMac())).willReturn(null)
        given(findAuthorizationTokenPort.findByAuthorizedDeviceMac(ticketMac())).willReturn(token)

        guard.refreshAuthenticatedClientFingerprint(ticketMac(), breachedFingerprint())

        verify(modifyAuthorizationTokenPort).save(token)
        assertEquals(now, token.authorizedDevices.single().reauthRequiredAt)
        assertEquals(
            "hash-a",
            token.authorizedDevices
                .single()
                .fingerprintProfile
                ?.signals
                ?.get(DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH)
                ?.value,
        )
        assertEquals(
            "android-dhcp-14",
            token.authorizedDevices
                .single()
                .fingerprintProfile
                ?.signals
                ?.get(DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS)
                ?.value,
        )
        assertEquals(
            "office-laptop",
            token.authorizedDevices
                .single()
                .fingerprintProfile
                ?.signals
                ?.get(DeviceFingerprintService.SIGNAL_HOSTNAME_STEM)
                ?.value,
        )
        assertEquals(Instant.parse("2025-01-01T10:00:00Z"), token.authorizedDevices.single().fingerprintVerifiedAt)
        verifyNoInteractions(networkUserDeviceWritePort)
        verify(eventPublisher).publish(MacAuthorizationStateChangedEvent(listOf(ticketMac())))
        val eventCaptor = argumentCaptor<DeviceFingerprintMismatchDetectedEvent>()
        verify(eventPublisher).publish(eventCaptor.capture())
        assertEquals(true, eventCaptor.firstValue.breached)
    }

    @Test
    fun `authenticated session active ticket device refreshes observed signals after verification`() {
        val token = ticketToken(ticketDevice(fingerprintProfile = acceptedEnforceableFingerprint()))
        given(timeProvider.get()).willReturn(now)
        given(networkUserDeviceReadPort.findByMac(ticketMac())).willReturn(null)
        given(findAuthorizationTokenPort.findByAuthorizedDeviceMac(ticketMac())).willReturn(token)

        guard.refreshAuthenticatedClientFingerprint(ticketMac(), activeRefreshFingerprint())

        verify(modifyAuthorizationTokenPort).save(token)
        assertNull(token.authorizedDevices.single().reauthRequiredAt)
        assertEquals(
            "hash-a",
            token.authorizedDevices
                .single()
                .fingerprintProfile
                ?.signals
                ?.get(DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH)
                ?.value,
        )
        assertEquals(
            "field-laptop",
            token.authorizedDevices
                .single()
                .fingerprintProfile
                ?.signals
                ?.get(DeviceFingerprintService.SIGNAL_HOSTNAME_STEM)
                ?.value,
        )
        assertEquals(now, token.authorizedDevices.single().fingerprintVerifiedAt)
        verifyNoInteractions(networkUserDeviceWritePort, eventPublisher)
    }

    private fun accountDevice(fingerprintProfile: DeviceFingerprintProfile?): NetworkUserDevice =
        NetworkUserDevice(
            userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000111")),
            mac = accountMac(),
            name = "Work Laptop",
            hostname = "office-laptop",
            isRandomized = false,
            authorizedAt = Instant.parse("2025-01-01T10:00:00Z"),
            lastSeenAt = Instant.parse("2025-01-01T10:10:00Z"),
            fingerprintProfile = fingerprintProfile,
            fingerprintStatus = deviceFingerprintService.status(fingerprintProfile),
            fingerprintVerifiedAt = Instant.parse("2025-01-01T10:00:00Z"),
            reauthRequiredAt = null,
        )

    private fun ticketToken(device: Device): AuthorizationToken =
        AuthorizationToken(
            id = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000222")),
            accessCode = "ABCDEFGH",
            validUntil = Instant.parse("2025-01-02T10:00:00Z"),
            requireUserNameOnLogin = false,
            authorizedDevices = mutableListOf(device),
            kickedMacAddresses = mutableSetOf(),
        )

    private fun ticketDevice(fingerprintProfile: DeviceFingerprintProfile?): Device =
        Device(
            mac = ticketMac(),
            displayName = "Guest User",
            deviceName = "guest-phone",
            fingerprintProfile = fingerprintProfile,
            fingerprintStatus = deviceFingerprintService.status(fingerprintProfile),
            fingerprintVerifiedAt = Instant.parse("2025-01-01T10:00:00Z"),
            reauthRequiredAt = null,
        )

    private fun matchingFingerprint(): DeviceFingerprintProfile = acceptedEnforceableFingerprint()

    private fun alarmOnlyFingerprint(): DeviceFingerprintProfile =
        profile(
            DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to
                signal("hash-a", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.STRONG),
            DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS to
                signal("android-dhcp-15", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.MEDIUM),
            DeviceFingerprintService.SIGNAL_HOSTNAME_STEM to
                signal("guest-phone", "router-agent:hostname", DeviceFingerprintSignalStrength.WEAK),
        )

    private fun breachedFingerprint(): DeviceFingerprintProfile =
        profile(
            DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to
                signal("hash-b", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.STRONG),
            DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS to
                signal("android-dhcp-15", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.MEDIUM),
        )

    private fun activeRefreshFingerprint(): DeviceFingerprintProfile =
        profile(
            DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to
                signal("hash-a", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.STRONG),
            DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS to
                signal("android-dhcp-14", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.MEDIUM),
            DeviceFingerprintService.SIGNAL_HOSTNAME_STEM to
                signal("field-laptop", "router-agent:hostname", DeviceFingerprintSignalStrength.WEAK),
        )

    private fun acceptedEnforceableFingerprint(): DeviceFingerprintProfile =
        profile(
            DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to
                signal("hash-a", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.STRONG),
            DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS to
                signal("android-dhcp-14", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.MEDIUM),
            DeviceFingerprintService.SIGNAL_HOSTNAME_STEM to
                signal("office-laptop", "router-agent:hostname", DeviceFingerprintSignalStrength.WEAK),
        )

    private fun profile(vararg entries: Pair<String, DeviceFingerprintSignal>): DeviceFingerprintProfile =
        DeviceFingerprintProfile(signals = linkedMapOf(*entries))

    private fun signal(
        value: String,
        source: String,
        strength: DeviceFingerprintSignalStrength,
    ) = DeviceFingerprintSignal(value = value, source = source, strength = strength)

    private fun accountMac(): String = "aa:bb:cc:dd:ee:01"

    private fun ticketMac(): String = "aa:bb:cc:dd:ee:02"
}
