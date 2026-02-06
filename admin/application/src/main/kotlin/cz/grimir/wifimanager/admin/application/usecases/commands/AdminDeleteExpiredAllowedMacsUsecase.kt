package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ports.DeleteAllowedMacPort
import cz.grimir.wifimanager.admin.application.ports.FindAllowedMacPort
import cz.grimir.wifimanager.shared.events.AllowedMacRemovedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Service
class AdminDeleteExpiredAllowedMacsUsecase(
    private val findAllowedMacPort: FindAllowedMacPort,
    private val deleteAllowedMacPort: DeleteAllowedMacPort,
    private val eventPublisher: AdminEventPublisher,
) {
    fun deleteExpired(now: Instant) {
        val expired = findAllowedMacPort.findExpired(now)
        if (expired.isEmpty()) {
            return
        }

        expired.forEach { allowedMac ->
            deleteAllowedMacPort.deleteByMac(allowedMac.mac)
            eventPublisher.publish(
                AllowedMacRemovedEvent(
                    macAddress = allowedMac.mac,
                    removedByUserId = null,
                    removedAt = now,
                ),
            )
        }

        logger.info { "Expired allowed macs removed count=${expired.size}" }
    }
}
