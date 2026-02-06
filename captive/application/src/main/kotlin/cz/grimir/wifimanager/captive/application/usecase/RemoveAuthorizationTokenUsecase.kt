package cz.grimir.wifimanager.captive.application.usecase

import cz.grimir.wifimanager.captive.application.command.RemoveAuthorizationTokenCommand
import cz.grimir.wifimanager.captive.application.ports.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.ports.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.ports.ModifyAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.ports.RouterAgentPort
import cz.grimir.wifimanager.captive.core.value.Device
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class RemoveAuthorizationTokenUsecase(
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort,
    private val modifyAuthorizationTokenPort: ModifyAuthorizationTokenPort,
    private val allowedMacReadPort: AllowedMacReadPort,
    private val localRouterAgentPort: RouterAgentPort,
) {
    @Transactional
    fun remove(command: RemoveAuthorizationTokenCommand) {
        val token = findAuthorizationTokenPort.findByTicketId(command.ticketId)

        if (token == null) {
            logger.warn { "Token id=${command.ticketId} not found" }
            return
        }

        modifyAuthorizationTokenPort.deleteByTicketId(command.ticketId)

        val allowedMacs = allowedMacReadPort.findAllMacs().toSet()
        val macsToRevoke = token.authorizedDevices.map(Device::mac).filterNot(allowedMacs::contains)
        if (macsToRevoke.isNotEmpty()) {
            localRouterAgentPort.revokeClientAccess(macsToRevoke)
        }

        logger.info { "Removed authorization token for ticket id=${command.ticketId}" }
    }
}
