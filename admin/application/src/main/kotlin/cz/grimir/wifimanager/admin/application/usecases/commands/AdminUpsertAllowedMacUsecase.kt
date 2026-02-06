package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.commands.UpsertAllowedMacCommand
import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ports.FindAllowedMacPort
import cz.grimir.wifimanager.admin.application.ports.SaveAllowedMacPort
import cz.grimir.wifimanager.admin.core.value.AllowedMac
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.events.AllowedMacUpsertedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class AdminUpsertAllowedMacUsecase(
    private val findAllowedMacPort: FindAllowedMacPort,
    private val saveAllowedMacPort: SaveAllowedMacPort,
    private val eventPublisher: AdminEventPublisher,
    private val timeProvider: TimeProvider,
) {
    fun upsert(command: UpsertAllowedMacCommand) {
        val now = timeProvider.get()
        val validUntil = command.validityDuration?.let { now.plus(it) }
        val existing = findAllowedMacPort.findByMac(command.macAddress)

        val allowedMac =
            if (existing != null) {
                if (existing.ownerUserId != command.user.userId) {
                    logger.info { "Allowed mac owner changed mac=${existing.mac} from=${existing.ownerUserId} to=${command.user.userId}" }
                }
                existing.copy(
                    hostname = command.hostname ?: existing.hostname,
                    ownerUserId = command.user.userId,
                    ownerDisplayName = command.user.displayName,
                    ownerEmail = command.user.email,
                    note = command.note,
                    validUntil = validUntil,
                    updatedAt = now,
                )
            } else {
                AllowedMac(
                    mac = command.macAddress,
                    hostname = command.hostname,
                    ownerUserId = command.user.userId,
                    ownerDisplayName = command.user.displayName,
                    ownerEmail = command.user.email,
                    note = command.note,
                    validUntil = validUntil,
                    createdAt = now,
                    updatedAt = now,
                )
            }

        saveAllowedMacPort.save(allowedMac)

        eventPublisher.publish(
            AllowedMacUpsertedEvent(
                macAddress = allowedMac.mac,
                ownerUserId = allowedMac.ownerUserId,
                ownerDisplayName = allowedMac.ownerDisplayName,
                ownerEmail = allowedMac.ownerEmail,
                validUntil = allowedMac.validUntil,
                updatedAt = allowedMac.updatedAt,
            ),
        )

        logger.info { "Allowed mac upserted mac=${allowedMac.mac} owner=${allowedMac.ownerUserId} validUntil=${allowedMac.validUntil}" }
    }
}
