package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.commands.DeleteAllowedMacCommand
import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ports.DeleteAllowedMacPort
import cz.grimir.wifimanager.admin.application.ports.FindAllowedMacPort
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.events.AllowedMacRemovedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class AdminDeleteAllowedMacUsecase(
    private val findAllowedMacPort: FindAllowedMacPort,
    private val deleteAllowedMacPort: DeleteAllowedMacPort,
    private val eventPublisher: AdminEventPublisher,
    private val timeProvider: TimeProvider,
) {
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
