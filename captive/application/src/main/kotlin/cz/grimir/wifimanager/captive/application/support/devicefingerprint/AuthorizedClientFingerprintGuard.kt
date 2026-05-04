package cz.grimir.wifimanager.captive.application.support.devicefingerprint

import cz.grimir.wifimanager.captive.application.event.MacAuthorizationStateChangedEvent
import cz.grimir.wifimanager.captive.application.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.port.ModifyAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.query.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.captive.application.port.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import cz.grimir.wifimanager.captive.core.value.Device
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintComparison
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintProfile
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.events.DeviceFingerprintActionTaken
import cz.grimir.wifimanager.shared.events.DeviceFingerprintMismatchDetectedEvent
import cz.grimir.wifimanager.shared.events.DeviceFingerprintSubjectType
import org.springframework.stereotype.Service
import java.time.Instant

enum class AuthorizedMacState {
    NONE,
    ACTIVE_NETWORK_USER_DEVICE,
    ACTIVE_TICKET_DEVICE,
    REAUTH_REQUIRED,
}

data class AuthorizedMacVerification(
    val state: AuthorizedMacState,
    val networkUserDevice: NetworkUserDevice? = null,
    val token: AuthorizationToken? = null,
    val ticketDevice: Device? = null,
)

@Service
class AuthorizedClientFingerprintGuard(
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
    private val networkUserDeviceWritePort: NetworkUserDeviceWritePort,
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort,
    private val modifyAuthorizationTokenPort: ModifyAuthorizationTokenPort,
    private val eventPublisher: CaptiveEventPublisher,
    private val timeProvider: TimeProvider,
    private val deviceFingerprintService: DeviceFingerprintService,
) {
    fun verifyAuthorizedMac(
        mac: String,
        currentFingerprint: DeviceFingerprintProfile?,
    ): AuthorizedMacVerification {
        val now = timeProvider.get()
        val networkDevice = networkUserDeviceReadPort.findByMac(mac)
        if (networkDevice != null) {
            return verifyNetworkUserDevice(networkDevice, currentFingerprint, now)
        }

        val token = findAuthorizationTokenPort.findByAuthorizedDeviceMac(mac) ?: return AuthorizedMacVerification(AuthorizedMacState.NONE)
        val ticketDevice =
            token.authorizedDevices.firstOrNull { it.mac == mac }
                ?: return AuthorizedMacVerification(AuthorizedMacState.NONE)
        return verifyTicketDevice(token, ticketDevice, currentFingerprint, now)
    }

    fun processAuthorizedClientObservation(
        mac: String,
        currentFingerprint: DeviceFingerprintProfile?,
    ) {
        verifyAuthorizedMac(mac, currentFingerprint)
    }

    fun refreshAuthenticatedClientFingerprint(
        mac: String,
        currentFingerprint: DeviceFingerprintProfile?,
    ) {
        if (currentFingerprint == null) {
            return
        }

        val now = timeProvider.get()
        val networkDevice = networkUserDeviceReadPort.findByMac(mac)
        if (networkDevice != null) {
            val verification = verifyNetworkUserDevice(networkDevice, currentFingerprint, now)
            if (verification.state == AuthorizedMacState.ACTIVE_NETWORK_USER_DEVICE) {
                refreshNetworkUserDeviceFingerprint(verification.networkUserDevice ?: networkDevice, currentFingerprint, now)
            }
            return
        }

        val token = findAuthorizationTokenPort.findByAuthorizedDeviceMac(mac) ?: return
        val ticketDevice = token.authorizedDevices.firstOrNull { it.mac == mac } ?: return
        val verification = verifyTicketDevice(token, ticketDevice, currentFingerprint, now)
        if (verification.state == AuthorizedMacState.ACTIVE_TICKET_DEVICE) {
            refreshTicketDeviceFingerprint(verification.token ?: token, verification.ticketDevice ?: ticketDevice, currentFingerprint, now)
        }
    }

    private fun verifyNetworkUserDevice(
        device: NetworkUserDevice,
        currentFingerprint: DeviceFingerprintProfile?,
        now: Instant,
    ): AuthorizedMacVerification {
        if (device.reauthRequiredAt != null) {
            return AuthorizedMacVerification(AuthorizedMacState.REAUTH_REQUIRED, networkUserDevice = device)
        }

        val comparison = deviceFingerprintService.compare(device.fingerprintProfile, currentFingerprint)
        maybeLogMismatch(
            comparison = comparison,
            subjectType = DeviceFingerprintSubjectType.NETWORK_USER_DEVICE,
            userId = device.userId,
            ticketId = null,
            mac = device.mac,
            previous = device.fingerprintProfile,
            current = currentFingerprint,
            breached = comparison.breached,
            now = now,
        )

        if (comparison.breached) {
            val updated = device.copy(reauthRequiredAt = now)
            networkUserDeviceWritePort.save(updated)
            eventPublisher.publish(MacAuthorizationStateChangedEvent(listOf(device.mac)))
            return AuthorizedMacVerification(AuthorizedMacState.REAUTH_REQUIRED, networkUserDevice = updated)
        }

        if (comparison.reasons.isEmpty()) {
            val enriched = deviceFingerprintService.enrichMissingSignals(device.fingerprintProfile, currentFingerprint)
            if (enriched != device.fingerprintProfile) {
                val updated =
                    device.copy(
                        fingerprintProfile = enriched,
                        fingerprintStatus = deviceFingerprintService.status(enriched),
                    )
                networkUserDeviceWritePort.save(updated)
                return AuthorizedMacVerification(AuthorizedMacState.ACTIVE_NETWORK_USER_DEVICE, networkUserDevice = updated)
            }
        }

        return AuthorizedMacVerification(AuthorizedMacState.ACTIVE_NETWORK_USER_DEVICE, networkUserDevice = device)
    }

    private fun verifyTicketDevice(
        token: AuthorizationToken,
        device: Device,
        currentFingerprint: DeviceFingerprintProfile?,
        now: Instant,
    ): AuthorizedMacVerification {
        if (device.reauthRequiredAt != null) {
            return AuthorizedMacVerification(AuthorizedMacState.REAUTH_REQUIRED, token = token, ticketDevice = device)
        }

        val comparison = deviceFingerprintService.compare(device.fingerprintProfile, currentFingerprint)
        maybeLogMismatch(
            comparison = comparison,
            subjectType = DeviceFingerprintSubjectType.TICKET_DEVICE,
            userId = null,
            ticketId = token.id,
            mac = device.mac,
            previous = device.fingerprintProfile,
            current = currentFingerprint,
            breached = comparison.breached,
            now = now,
        )

        if (comparison.breached) {
            val updatedDevice = token.requireReauthForAuthorizedDevice(device.mac, now)
            modifyAuthorizationTokenPort.save(token)
            eventPublisher.publish(MacAuthorizationStateChangedEvent(listOf(device.mac)))
            return AuthorizedMacVerification(AuthorizedMacState.REAUTH_REQUIRED, token = token, ticketDevice = updatedDevice)
        }

        if (comparison.reasons.isEmpty()) {
            val enriched = deviceFingerprintService.enrichMissingSignals(device.fingerprintProfile, currentFingerprint)
            if (enriched != device.fingerprintProfile) {
                val updatedDevice =
                    token.updateAuthorizedDeviceFingerprint(
                        mac = device.mac,
                        fingerprintProfile = enriched,
                        fingerprintStatus = deviceFingerprintService.status(enriched),
                        fingerprintVerifiedAt = now,
                    )
                modifyAuthorizationTokenPort.save(token)
                return AuthorizedMacVerification(AuthorizedMacState.ACTIVE_TICKET_DEVICE, token = token, ticketDevice = updatedDevice)
            }
        }

        return AuthorizedMacVerification(AuthorizedMacState.ACTIVE_TICKET_DEVICE, token = token, ticketDevice = device)
    }

    private fun refreshNetworkUserDeviceFingerprint(
        device: NetworkUserDevice,
        currentFingerprint: DeviceFingerprintProfile,
        now: Instant,
    ) {
        if (device.reauthRequiredAt != null) {
            return
        }

        val merged = deviceFingerprintService.mergeObservedSignals(device.fingerprintProfile, currentFingerprint)
        if (device.fingerprintProfile == merged) {
            return
        }
        networkUserDeviceWritePort.save(
            device.copy(
                fingerprintProfile = merged,
                fingerprintStatus = deviceFingerprintService.status(merged),
                fingerprintVerifiedAt = now,
            ),
        )
    }

    private fun refreshTicketDeviceFingerprint(
        token: AuthorizationToken,
        device: Device,
        currentFingerprint: DeviceFingerprintProfile,
        now: Instant,
    ) {
        if (device.reauthRequiredAt != null) {
            return
        }

        val merged = deviceFingerprintService.mergeObservedSignals(device.fingerprintProfile, currentFingerprint)
        if (device.fingerprintProfile == merged) {
            return
        }
        token.updateAuthorizedDeviceFingerprint(
            mac = device.mac,
            fingerprintProfile = merged,
            fingerprintStatus = deviceFingerprintService.status(merged),
            fingerprintVerifiedAt = now,
        )
        modifyAuthorizationTokenPort.save(token)
    }

    private fun maybeLogMismatch(
        comparison: DeviceFingerprintComparison,
        subjectType: DeviceFingerprintSubjectType,
        userId: UserId?,
        ticketId: TicketId?,
        mac: String,
        previous: DeviceFingerprintProfile?,
        current: DeviceFingerprintProfile?,
        breached: Boolean,
        now: Instant,
    ) {
        if (!comparison.shouldLog && !breached) {
            return
        }

        eventPublisher.publish(
            DeviceFingerprintMismatchDetectedEvent(
                subjectType = subjectType,
                userId = userId,
                ticketId = ticketId,
                deviceMac = mac,
                score = comparison.score,
                breached = breached,
                actionTaken = if (breached) DeviceFingerprintActionTaken.REAUTH_REQUIRED else DeviceFingerprintActionTaken.LOG_ONLY,
                reasons = comparison.reasons,
                previousFingerprint = deviceFingerprintService.toJson(previous),
                currentFingerprint = deviceFingerprintService.toJson(current),
                previousSources = deviceFingerprintService.sourcesToJson(previous),
                currentSources = deviceFingerprintService.sourcesToJson(current),
                detectedAt = now,
            ),
        )
    }
}
