package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.command.handler.ScrubDeauthorizedCaptiveDeviceUsecase
import cz.grimir.wifimanager.captive.application.event.MacAuthorizationStateChangedEvent
import cz.grimir.wifimanager.captive.application.port.RouterAgentPort
import cz.grimir.wifimanager.captive.application.support.ClientAccessAuthorizationResolver
import cz.grimir.wifimanager.shared.application.network.MacAddressNormalizer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val logger = KotlinLogging.logger {}

@Component
class MacAuthorizationStateChangedEventListener(
    private val clientAccessAuthorizationResolver: ClientAccessAuthorizationResolver,
    private val routerAgentPort: RouterAgentPort,
    private val scrubDeauthorizedCaptiveDeviceUsecase: ScrubDeauthorizedCaptiveDeviceUsecase,
) {
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun on(event: MacAuthorizationStateChangedEvent) {
        val macAddresses =
            event.macAddresses
                .map(MacAddressNormalizer::normalize)
                .distinct()

        logger.trace { "Reconciling client authorization state macAddresses=$macAddresses" }
        macAddresses.forEach(::reconcile)
    }

    private fun reconcile(mac: String) {
        try {
            if (!isAuthorized(mac)) {
                routerAgentPort.revokeClientAccess(listOf(mac))
                scrub(mac)
                logTrace(mac, "revoked")
            } else {
                logTrace(mac, "remains authorized")
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to reconcile router access for mac=$mac" }
        }
    }

    private fun scrub(mac: String) {
        try {
            scrubDeauthorizedCaptiveDeviceUsecase.scrubIfEligible(mac)
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to scrub captive device PII for mac=$mac" }
        }
    }

    private fun logTrace(
        mac: String,
        action: String,
    ) {
        logger.trace {
            val isAllowedMac = clientAccessAuthorizationResolver.isAuthorizedByAllowedMac(mac)
            val isActiveTicketDevice = clientAccessAuthorizationResolver.isAuthorizedByActiveTicketDevice(mac)
            val isAccountDevice = clientAccessAuthorizationResolver.isAuthorizedByAccountDevice(mac)

            "Client access $action mac=$mac " +
                "allowedMac=$isAllowedMac " +
                "ticketDevice=$isActiveTicketDevice " +
                "accountDevice=$isAccountDevice"
        }
    }

    private fun isAuthorized(mac: String): Boolean =
        clientAccessAuthorizationResolver.isAuthorizedByAllowedMac(mac) ||
            clientAccessAuthorizationResolver.isAuthorizedByActiveTicketDevice(mac) ||
            clientAccessAuthorizationResolver.isAuthorizedByAccountDevice(mac)
}
