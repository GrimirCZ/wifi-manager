package cz.grimir.wifimanager.admin.application.allowedmac.handler.command

import cz.grimir.wifimanager.admin.application.allowedmac.command.DeleteAllowedMacCommand
import cz.grimir.wifimanager.admin.application.shared.port.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.allowedmac.port.DeleteAllowedMacPort
import cz.grimir.wifimanager.admin.application.allowedmac.port.FindAllowedMacPort
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.events.AllowedMacRemovedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class AdminDeleteAllowedMacUsecase(
    private val findAllowedMacPort: FindAllowedMacPort,
    private val deleteAllowedMacPort: DeleteAllowedMacPort,
    private val eventPublisher: AdminEventPublisher,
    private val timeProvider: TimeProvider,
) {
    @Transactional
    fun delete(command: DeleteAllowedMacCommand) {
        val existing = findAllowedMacPort.findByMac(command.macAddress) ?: return

        deleteAllowedMacPort.deleteByMac(existing.mac)

        eventPublisher.publish(
            AllowedMacRemovedEvent(
                macAddress = existing.mac,
                removedByUserId = command.user.userId,
                removedAt = timeProvider.get(),
            ),
        )

        logger.info { "Allowed mac removed mac=${existing.mac} removedBy=${command.user.userId}" }
    }
}
