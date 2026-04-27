package cz.grimir.wifimanager.captive.application.devicefingerprint

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintComparison
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintProfile
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintSignal
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintSignalStrength
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintStatus
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class DeviceFingerprintService(
    private val objectMapper: ObjectMapper,
    private val properties: CaptiveFingerprintingProperties,
    private val userAgentClassifier: UserAgentClassifier,
) {
    fun fromJsonNode(node: JsonNode?): DeviceFingerprintProfile? =
        node?.let { objectMapper.treeToValue(it, DeviceFingerprintProfile::class.java) }

    fun toJsonNode(profile: DeviceFingerprintProfile?): JsonNode? = profile?.let(objectMapper::valueToTree)

    fun fromJson(value: String?): DeviceFingerprintProfile? =
        value
            ?.takeIf { it.isNotBlank() }
            ?.let { objectMapper.readValue<DeviceFingerprintProfile>(it) }

    fun toJson(profile: DeviceFingerprintProfile?): String? =
        profile
            ?.takeIf { it.signals.isNotEmpty() }
            ?.let(objectMapper::writeValueAsString)

    fun sourcesToJson(profile: DeviceFingerprintProfile?): String? =
        profile
            ?.signals
            ?.takeIf { it.isNotEmpty() }
            ?.mapValues { (_, signal) -> signal.source }
            ?.let(objectMapper::writeValueAsString)

    fun status(profile: DeviceFingerprintProfile?): DeviceFingerprintStatus {
        if (profile == null || profile.signals.isEmpty()) {
            return DeviceFingerprintStatus.NONE
        }

        var strong = 0
        var mediumOrBetter = 0
        profile.signals.values.forEach { signal ->
            when (signal.strength) {
                DeviceFingerprintSignalStrength.STRONG -> {
                    strong += 1
                    mediumOrBetter += 1
                }

                DeviceFingerprintSignalStrength.MEDIUM -> mediumOrBetter += 1
                DeviceFingerprintSignalStrength.WEAK -> Unit
            }
        }

        return when {
            strong >= 1 || mediumOrBetter >= 2 -> DeviceFingerprintStatus.ENFORCEABLE
            else -> DeviceFingerprintStatus.PARTIAL
        }
    }

    fun compare(
        accepted: DeviceFingerprintProfile?,
        current: DeviceFingerprintProfile?,
    ): DeviceFingerprintComparison {
        val acceptedStatus = status(accepted)
        if (!properties.enabled || accepted == null || current == null || accepted.signals.isEmpty() || current.signals.isEmpty()) {
            return DeviceFingerprintComparison(
                status = acceptedStatus,
                score = 0,
                breached = false,
                shouldLog = false,
                reasons = emptyList(),
                matchedSignals = 0,
            )
        }

        var matchedSignals = 0
        var strongOverlap = 0
        var mediumOrBetterOverlap = 0
        var score = 0
        val reasons = mutableListOf<String>()
        var weakMismatchOnly = true

        accepted.signals.forEach { (name, acceptedSignal) ->
            val currentSignal = current.signals[name] ?: return@forEach
            matchedSignals += 1
            when (acceptedSignal.strength) {
                DeviceFingerprintSignalStrength.STRONG -> {
                    strongOverlap += 1
                    mediumOrBetterOverlap += 1
                }

                DeviceFingerprintSignalStrength.MEDIUM -> mediumOrBetterOverlap += 1
                DeviceFingerprintSignalStrength.WEAK -> Unit
            }
            if (acceptedSignal.value == currentSignal.value) {
                return@forEach
            }
            val delta =
                when (name) {
                    SIGNAL_TLS -> 70
                    SIGNAL_DHCP_PRL_HASH -> 55
                    SIGNAL_DHCP_VENDOR_CLASS -> 35
                    SIGNAL_UA_FAMILY_MAJOR -> 20
                    SIGNAL_HOSTNAME_STEM -> 10
                    SIGNAL_EXACT_HOSTNAME -> 5
                    else -> 0
                }
            if (delta <= 0) {
                return@forEach
            }
            score += delta
            reasons += "$name-mismatch"
            if (acceptedSignal.strength != DeviceFingerprintSignalStrength.WEAK) {
                weakMismatchOnly = false
            }
        }

        val overlapEnough = strongOverlap >= 1 || mediumOrBetterOverlap >= 2
        if (matchedSignals == 0 || !overlapEnough) {
            return DeviceFingerprintComparison(
                status = acceptedStatus,
                score = score,
                breached = false,
                shouldLog = score >= properties.logThreshold,
                reasons = if (score > 0) reasons + "insufficient-evidence" else emptyList(),
                matchedSignals = matchedSignals,
            )
        }

        val breached =
            acceptedStatus == DeviceFingerprintStatus.ENFORCEABLE &&
                !weakMismatchOnly &&
                score >= properties.enforceThreshold

        return DeviceFingerprintComparison(
            status = acceptedStatus,
            score = score,
            breached = breached,
            shouldLog = score >= properties.logThreshold,
            reasons = reasons,
            matchedSignals = matchedSignals,
        )
    }

    fun enrichMissingSignals(
        accepted: DeviceFingerprintProfile?,
        current: DeviceFingerprintProfile?,
    ): DeviceFingerprintProfile? {
        if (accepted == null) {
            return current
        }
        if (current == null || current.signals.isEmpty()) {
            return accepted
        }

        val merged = accepted.signals.toMutableMap()
        current.signals.forEach { (name, signal) ->
            merged.putIfAbsent(name, signal)
        }
        return accepted.copy(signals = merged.toMap())
    }

    fun mergeObservedSignals(
        accepted: DeviceFingerprintProfile?,
        current: DeviceFingerprintProfile?,
    ): DeviceFingerprintProfile? {
        if (accepted == null) {
            return current
        }
        if (current == null || current.signals.isEmpty()) {
            return accepted
        }

        val merged = accepted.signals.toMutableMap()
        current.signals.forEach { (name, signal) ->
            merged[name] = signal
        }
        return accepted.copy(signals = merged.toMap())
    }

    fun createHttpObservation(
        routerHostname: String?,
        dhcpHostname: String?,
        dhcpVendorClass: String?,
        dhcpPrlHash: String?,
        tlsFingerprint: String?,
        userAgent: String?,
    ): DeviceFingerprintProfile? {
        val signals = linkedMapOf<String, DeviceFingerprintSignal>()
        normalized(tlsFingerprint)?.let {
            signals[SIGNAL_TLS] =
                DeviceFingerprintSignal(
                    value = it,
                    source = "nginx:${properties.trustedTlsHeaderName.lowercase(Locale.ROOT)}",
                    strength = DeviceFingerprintSignalStrength.STRONG,
                )
        }
        normalized(dhcpPrlHash)?.let {
            signals[SIGNAL_DHCP_PRL_HASH] =
                DeviceFingerprintSignal(
                    value = it,
                    source = "router-agent:dhcp-log",
                    strength = DeviceFingerprintSignalStrength.STRONG,
                )
        }
        normalized(dhcpVendorClass)?.let {
            signals[SIGNAL_DHCP_VENDOR_CLASS] =
                DeviceFingerprintSignal(
                    value = it,
                    source = "router-agent:dhcp-log",
                    strength = DeviceFingerprintSignalStrength.MEDIUM,
                )
        }
        userAgentClassifier.classify(userAgent)?.let {
            signals[SIGNAL_UA_FAMILY_MAJOR] =
                DeviceFingerprintSignal(
                    value = it,
                    source = "http:user-agent",
                    strength = DeviceFingerprintSignalStrength.MEDIUM,
                )
        }

        val hostname = normalized(dhcpHostname) ?: normalized(routerHostname)
        hostname?.let {
            signals[SIGNAL_EXACT_HOSTNAME] =
                DeviceFingerprintSignal(
                    value = it,
                    source = if (normalized(dhcpHostname) != null) "router-agent:dhcp-log" else "router-agent:hostname",
                    strength = DeviceFingerprintSignalStrength.WEAK,
                )
            normalizeHostnameStem(it)?.let { stem ->
                signals[SIGNAL_HOSTNAME_STEM] =
                    DeviceFingerprintSignal(
                        value = stem,
                        source = if (normalized(dhcpHostname) != null) "router-agent:dhcp-log" else "router-agent:hostname",
                        strength = DeviceFingerprintSignalStrength.WEAK,
                    )
            }
        }

        return signals.takeIf { it.isNotEmpty() }?.let { DeviceFingerprintProfile(signals = it.toMap()) }
    }

    fun createRouterObservation(
        hostname: String?,
        dhcpHostname: String?,
        dhcpVendorClass: String?,
        dhcpPrlHash: String?,
    ): DeviceFingerprintProfile? = createHttpObservation(hostname, dhcpHostname, dhcpVendorClass, dhcpPrlHash, null, null)

    fun trustedTlsFingerprint(headerValue: String?): String? {
        if (!properties.enabled || !properties.trustedProxyEnabled) {
            return null
        }
        return normalized(headerValue)
    }

    private fun normalized(value: String?): String? = value?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() }

    private fun normalizeHostnameStem(hostname: String): String? =
        hostname
            .substringBefore('.')
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .takeIf { it.isNotBlank() }

    companion object {
        const val SIGNAL_TLS = "tls"
        const val SIGNAL_DHCP_VENDOR_CLASS = "dhcp_vendor_class"
        const val SIGNAL_DHCP_PRL_HASH = "dhcp_prl_hash"
        const val SIGNAL_UA_FAMILY_MAJOR = "ua_family_major"
        const val SIGNAL_HOSTNAME_STEM = "hostname_stem"
        const val SIGNAL_EXACT_HOSTNAME = "exact_hostname"
    }
}
