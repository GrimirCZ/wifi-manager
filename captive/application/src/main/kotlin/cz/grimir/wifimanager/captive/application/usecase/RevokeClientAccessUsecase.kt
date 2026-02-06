package cz.grimir.wifimanager.captive.application.usecase

import cz.grimir.wifimanager.captive.application.command.RevokeClientAccessCommand
import cz.grimir.wifimanager.captive.application.ports.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.ports.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.application.ports.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.ports.ModifyAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.ports.RouterAgentPort
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.events.ClientAccessRevokedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class RevokeClientAccessUsecase(
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort,
    private val modifyAuthorizationTokenPort: ModifyAuthorizationTokenPort,
    private val allowedMacReadPort: AllowedMacReadPort,
    private val routerAgentPort: RouterAgentPort,
    private val eventPublisher: CaptiveEventPublisher,
    private val timeProvider: TimeProvider,
) {
    fun revoke(command: RevokeClientAccessCommand) {
        val token = findAuthorizationTokenPort.findByTicketId(command.ticketId)
        if (token == null) {
            logger.warn { "Token id=${command.ticketId} not found" }
            return
        }

        token.kickedMacAddresses.add(command.deviceMacAddress)
        modifyAuthorizationTokenPort.save(token)

        val isAllowed = allowedMacReadPort.findByMac(command.deviceMacAddress) != null
        if (!isAllowed) {
            routerAgentPort.revokeClientAccess(listOf(command.deviceMacAddress))
        }

        eventPublisher.publish(
            ClientAccessRevokedEvent(
                ticketId = command.ticketId,
                deviceMacAddress = command.deviceMacAddress,
                revokedAt = timeProvider.get(),
            ),
        )
    }
}
