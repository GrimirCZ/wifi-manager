package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.authorization.event.MacAuthorizationStateChangedEvent
import cz.grimir.wifimanager.captive.application.authorization.support.ClientAccessAuthorizationResolver
import cz.grimir.wifimanager.captive.application.integration.routeragent.port.RouterAgentPort
import cz.grimir.wifimanager.shared.application.network.MacAddressNormalizer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val logger = KotlinLogging.logger {}

@Component
class MacAuthorizationStateChangedEventListener(
    private val clientAccessAuthorizationResolver: ClientAccessAuthorizationResolver,
    private val routerAgentPort: RouterAgentPort,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: MacAuthorizationStateChangedEvent) {
        event.macAddresses
            .map(MacAddressNormalizer::normalize)
            .distinct()
            .forEach(::reconcile)
    }

    private fun reconcile(mac: String) {
        try {
            if (!isAuthorized(mac)) {
                routerAgentPort.revokeClientAccess(listOf(mac))
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to reconcile router access for mac=$mac" }
        }
    }

    private fun isAuthorized(mac: String): Boolean =
        clientAccessAuthorizationResolver.isAuthorizedByAllowedMac(mac) ||
            clientAccessAuthorizationResolver.isAuthorizedByActiveTicketDevice(mac) ||
            clientAccessAuthorizationResolver.isAuthorizedByAccountDevice(mac)
}
