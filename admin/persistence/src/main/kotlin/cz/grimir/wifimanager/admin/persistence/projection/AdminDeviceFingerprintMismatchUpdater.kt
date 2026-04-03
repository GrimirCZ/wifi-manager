package cz.grimir.wifimanager.admin.persistence.projection

import com.fasterxml.jackson.databind.ObjectMapper
import cz.grimir.wifimanager.admin.persistence.AdminDeviceFingerprintMismatchJpaRepository
import cz.grimir.wifimanager.admin.persistence.entity.AdminDeviceFingerprintMismatchEntity
import cz.grimir.wifimanager.shared.events.DeviceFingerprintMismatchDetectedEvent
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AdminDeviceFingerprintMismatchUpdater(
    private val repository: AdminDeviceFingerprintMismatchJpaRepository,
    private val objectMapper: ObjectMapper,
) {
    fun append(event: DeviceFingerprintMismatchDetectedEvent) {
        repository.save(
            AdminDeviceFingerprintMismatchEntity(
                id = UUID.randomUUID(),
                subjectType = event.subjectType.name,
                userId = event.userId?.id,
                ticketId = event.ticketId?.id,
                deviceMac = event.deviceMac,
                score = event.score,
                breached = event.breached,
                actionTaken = event.actionTaken.name,
                reasons = event.reasons.toTypedArray(),
                previousFingerprint = event.previousFingerprint?.let(objectMapper::readTree),
                currentFingerprint = event.currentFingerprint?.let(objectMapper::readTree),
                previousSources = event.previousSources?.let(objectMapper::readTree),
                currentSources = event.currentSources?.let(objectMapper::readTree),
                detectedAt = event.detectedAt,
            ),
        )
    }
}
