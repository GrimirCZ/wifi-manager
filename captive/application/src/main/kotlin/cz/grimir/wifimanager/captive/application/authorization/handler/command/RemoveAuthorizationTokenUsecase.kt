package cz.grimir.wifimanager.captive.application.authorization.handler.command

import cz.grimir.wifimanager.captive.application.authorization.command.RemoveAuthorizationTokenCommand
import cz.grimir.wifimanager.captive.application.authorization.event.MacAuthorizationStateChangedEvent
import cz.grimir.wifimanager.captive.application.authorization.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.authorization.port.ModifyAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.shared.port.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.core.value.Device
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class RemoveAuthorizationTokenUsecase(
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort,
    private val modifyAuthorizationTokenPort: ModifyAuthorizationTokenPort,
    private val captiveEventPublisher: CaptiveEventPublisher,
) {
    @Transactional
    fun remove(command: RemoveAuthorizationTokenCommand) {
        val token = findAuthorizationTokenPort.findByTicketId(command.ticketId)

        if (token == null) {
            logger.warn { "Token id=${command.ticketId} not found" }
            return
        }

        modifyAuthorizationTokenPort.deleteByTicketId(command.ticketId)

        captiveEventPublisher.publish(
            MacAuthorizationStateChangedEvent(
                token.authorizedDevices
                    .map(Device::mac)
                    .distinct(),
            ),
        )

        logger.debug { "Removed authorization token for ticket id=${command.ticketId}" }
    }
}
