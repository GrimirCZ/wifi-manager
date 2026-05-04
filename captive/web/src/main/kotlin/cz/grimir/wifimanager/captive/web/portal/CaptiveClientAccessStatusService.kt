package cz.grimir.wifimanager.captive.web.portal

import cz.grimir.wifimanager.captive.application.port.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.query.model.AllowedMac
import cz.grimir.wifimanager.captive.application.query.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.AuthorizedClientFingerprintGuard
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.AuthorizedMacState
import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import cz.grimir.wifimanager.captive.core.value.Device
import cz.grimir.wifimanager.captive.web.security.support.ClientInfo
import cz.grimir.wifimanager.shared.core.TimeProvider
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

enum class CaptiveClientAccessState {
    UNAUTHORIZED,
    ACTIVE_NETWORK_USER_DEVICE,
    ACTIVE_TICKET_DEVICE,
    KICKED_TICKET_DEVICE,
    REAUTH_REQUIRED_NETWORK_USER_DEVICE,
    REAUTH_REQUIRED_TICKET_DEVICE,
    ACTIVE_ALLOWED_MAC,
    EXPIRED_ALLOWED_MAC,
}

data class CaptiveClientAccessStatus(
    val state: CaptiveClientAccessState,
    val networkUserDevice: NetworkUserDevice? = null,
    val token: AuthorizationToken? = null,
    val ticketDevice: Device? = null,
    val allowedMac: AllowedMac? = null,
    val secondsRemaining: Long? = null,
) {
    val captive: Boolean
        get() =
            when (state) {
                CaptiveClientAccessState.ACTIVE_NETWORK_USER_DEVICE,
                CaptiveClientAccessState.ACTIVE_TICKET_DEVICE,
                CaptiveClientAccessState.ACTIVE_ALLOWED_MAC,
                -> false

                CaptiveClientAccessState.UNAUTHORIZED,
                CaptiveClientAccessState.KICKED_TICKET_DEVICE,
                CaptiveClientAccessState.REAUTH_REQUIRED_NETWORK_USER_DEVICE,
                CaptiveClientAccessState.REAUTH_REQUIRED_TICKET_DEVICE,
                CaptiveClientAccessState.EXPIRED_ALLOWED_MAC,
                -> true
            }
}

@Service
class CaptiveClientAccessStatusService(
    private val authorizedClientFingerprintGuard: AuthorizedClientFingerprintGuard,
    private val allowedMacReadPort: AllowedMacReadPort,
    private val timeProvider: TimeProvider,
) {
    fun resolve(clientInfo: ClientInfo): CaptiveClientAccessStatus {
        val now = timeProvider.get()
        val verification = authorizedClientFingerprintGuard.verifyAuthorizedMac(clientInfo.macAddress, clientInfo.fingerprintProfile)

        when (verification.state) {
            AuthorizedMacState.ACTIVE_NETWORK_USER_DEVICE ->
                return CaptiveClientAccessStatus(
                    state = CaptiveClientAccessState.ACTIVE_NETWORK_USER_DEVICE,
                    networkUserDevice = verification.networkUserDevice,
                )

            AuthorizedMacState.REAUTH_REQUIRED ->
                return if (verification.networkUserDevice != null) {
                    CaptiveClientAccessStatus(
                        state = CaptiveClientAccessState.REAUTH_REQUIRED_NETWORK_USER_DEVICE,
                        networkUserDevice = verification.networkUserDevice,
                    )
                } else {
                    CaptiveClientAccessStatus(
                        state = CaptiveClientAccessState.REAUTH_REQUIRED_TICKET_DEVICE,
                        token = verification.token,
                        ticketDevice = verification.ticketDevice,
                    )
                }

            AuthorizedMacState.ACTIVE_TICKET_DEVICE -> {
                val token = verification.token
                val ticketDevice = verification.ticketDevice
                if (token != null && ticketDevice != null && token.validUntil.isAfter(now)) {
                    if (token.kickedMacAddresses.contains(clientInfo.macAddress)) {
                        return CaptiveClientAccessStatus(
                            state = CaptiveClientAccessState.KICKED_TICKET_DEVICE,
                            token = token,
                            ticketDevice = ticketDevice,
                        )
                    }
                    return CaptiveClientAccessStatus(
                        state = CaptiveClientAccessState.ACTIVE_TICKET_DEVICE,
                        token = token,
                        ticketDevice = ticketDevice,
                        secondsRemaining = secondsRemaining(now, token.validUntil),
                    )
                }
            }

            AuthorizedMacState.NONE -> {
                // Fall through to allowed-MAC resolution below.
            }
        }

        val allowedMac = allowedMacReadPort.findByMac(clientInfo.macAddress)
        if (allowedMac != null) {
            val validUntil = allowedMac.validUntil
            if (validUntil == null) {
                return CaptiveClientAccessStatus(
                    state = CaptiveClientAccessState.ACTIVE_ALLOWED_MAC,
                    allowedMac = allowedMac,
                )
            }
            if (validUntil.isAfter(now)) {
                return CaptiveClientAccessStatus(
                    state = CaptiveClientAccessState.ACTIVE_ALLOWED_MAC,
                    allowedMac = allowedMac,
                    secondsRemaining = secondsRemaining(now, validUntil),
                )
            }
            return CaptiveClientAccessStatus(
                state = CaptiveClientAccessState.EXPIRED_ALLOWED_MAC,
                allowedMac = allowedMac,
            )
        }

        return CaptiveClientAccessStatus(CaptiveClientAccessState.UNAUTHORIZED)
    }

    private fun secondsRemaining(
        now: Instant,
        validUntil: Instant,
    ): Long? = Duration.between(now, validUntil).seconds.takeIf { it > 0 }
}
