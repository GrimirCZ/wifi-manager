package cz.grimir.wifimanager.captive.application.authorization.handler.command

import cz.grimir.wifimanager.captive.application.authorization.command.RevokeClientAccessCommand
import cz.grimir.wifimanager.captive.application.authorization.event.MacAuthorizationStateChangedEvent
import cz.grimir.wifimanager.captive.application.authorization.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.authorization.port.ModifyAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.shared.port.CaptiveEventPublisher
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.events.ClientAccessRevokedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class RevokeClientAccessUsecase(
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort,
    private val modifyAuthorizationTokenPort: ModifyAuthorizationTokenPort,
    private val eventPublisher: CaptiveEventPublisher,
    private val timeProvider: TimeProvider,
) {
    @Transactional
    fun revoke(command: RevokeClientAccessCommand) {
        val token = findAuthorizationTokenPort.findByTicketId(command.ticketId)
        if (token == null) {
            logger.warn { "Token id=${command.ticketId} not found" }
            return
        }

        token.kickedMacAddresses.add(command.deviceMacAddress)
        modifyAuthorizationTokenPort.save(token)

        eventPublisher.publish(
            ClientAccessRevokedEvent(
                ticketId = command.ticketId,
                deviceMacAddress = command.deviceMacAddress,
                revokedAt = timeProvider.get(),
            ),
        )
        eventPublisher.publish(MacAuthorizationStateChangedEvent(listOf(command.deviceMacAddress)))
    }
}
