package cz.grimir.wifimanager.admin.application.command.handler

import cz.grimir.wifimanager.admin.application.port.DeleteAllowedMacPort
import cz.grimir.wifimanager.admin.application.port.FindAllowedMacPort
import cz.grimir.wifimanager.admin.application.port.AdminEventPublisher
import cz.grimir.wifimanager.shared.events.AllowedMacRemovedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Service
class AdminDeleteExpiredAllowedMacsUsecase(
    private val findAllowedMacPort: FindAllowedMacPort,
    private val deleteAllowedMacPort: DeleteAllowedMacPort,
    private val eventPublisher: AdminEventPublisher,
) {
    @Transactional
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
