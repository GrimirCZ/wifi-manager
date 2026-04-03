package cz.grimir.wifimanager.admin.events.local

import cz.grimir.wifimanager.admin.persistence.projection.AdminDeviceFingerprintMismatchUpdater
import cz.grimir.wifimanager.shared.events.DeviceFingerprintMismatchDetectedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AdminDeviceFingerprintMismatchDetectedEventListener(
    private val updater: AdminDeviceFingerprintMismatchUpdater,
) {
    @EventListener
    fun on(event: DeviceFingerprintMismatchDetectedEvent) {
        updater.append(event)
    }
}
