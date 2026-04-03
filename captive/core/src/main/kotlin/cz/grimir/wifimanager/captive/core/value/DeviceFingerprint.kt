package cz.grimir.wifimanager.captive.core.value

import java.time.Instant

data class DeviceFingerprintProfile(
    val version: Int = 1,
    val signals: Map<String, DeviceFingerprintSignal> = emptyMap(),
)

data class DeviceFingerprintSignal(
    val value: String,
    val source: String,
    val strength: DeviceFingerprintSignalStrength,
)

enum class DeviceFingerprintSignalStrength {
    STRONG,
    MEDIUM,
    WEAK,
}

enum class DeviceFingerprintStatus {
    NONE,
    PARTIAL,
    ENFORCEABLE,
}

data class DeviceFingerprintComparison(
    val status: DeviceFingerprintStatus,
    val score: Int,
    val breached: Boolean,
    val shouldLog: Boolean,
    val reasons: List<String>,
    val matchedSignals: Int,
)

data class DeviceFingerprintMismatchRecord(
    val score: Int,
    val breached: Boolean,
    val reasons: List<String>,
    val previousFingerprint: String?,
    val currentFingerprint: String?,
    val previousSources: String?,
    val currentSources: String?,
    val detectedAt: Instant,
)
