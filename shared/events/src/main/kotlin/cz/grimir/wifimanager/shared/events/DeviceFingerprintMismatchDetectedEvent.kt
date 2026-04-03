package cz.grimir.wifimanager.shared.events

import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant

data class DeviceFingerprintMismatchDetectedEvent(
    val subjectType: DeviceFingerprintSubjectType,
    val userId: UserId?,
    val ticketId: TicketId?,
    val deviceMac: String,
    val score: Int,
    val breached: Boolean,
    val actionTaken: DeviceFingerprintActionTaken,
    val reasons: List<String>,
    val previousFingerprint: String?,
    val currentFingerprint: String?,
    val previousSources: String?,
    val currentSources: String?,
    val detectedAt: Instant,
)

enum class DeviceFingerprintSubjectType {
    NETWORK_USER_DEVICE,
    TICKET_DEVICE,
}

enum class DeviceFingerprintActionTaken {
    LOG_ONLY,
    REAUTH_REQUIRED,
}
