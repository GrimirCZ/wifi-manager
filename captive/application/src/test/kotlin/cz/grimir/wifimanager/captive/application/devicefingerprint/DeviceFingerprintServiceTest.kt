package cz.grimir.wifimanager.captive.application.devicefingerprint

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintProfile
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintSignal
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintSignalStrength
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceFingerprintServiceTest {
    private val service =
        DeviceFingerprintService(
            objectMapper = jacksonObjectMapper().findAndRegisterModules(),
            properties = CaptiveFingerprintingProperties(),
            userAgentClassifier = UserAgentClassifier { null },
        )

    @Test
    fun `identical enforceable fingerprints do not log or breach`() {
        val accepted = acceptedEnforceableFingerprint()
        val comparison = service.compare(accepted, acceptedEnforceableFingerprint())

        assertEquals(DeviceFingerprintStatus.ENFORCEABLE, comparison.status)
        assertEquals(0, comparison.score)
        assertFalse(comparison.shouldLog)
        assertFalse(comparison.breached)
        assertTrue(comparison.reasons.isEmpty())
    }

    @Test
    fun `weak only mismatch never breaches`() {
        val accepted =
            profile(
                DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to
                    signal("hash-a", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.STRONG),
                DeviceFingerprintService.SIGNAL_HOSTNAME_STEM to
                    signal("office-laptop", "router-agent:hostname", DeviceFingerprintSignalStrength.WEAK),
            )
        val current =
            profile(
                DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to
                    signal("hash-a", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.STRONG),
                DeviceFingerprintService.SIGNAL_HOSTNAME_STEM to
                    signal("guest-phone", "router-agent:hostname", DeviceFingerprintSignalStrength.WEAK),
            )

        val comparison = service.compare(accepted, current)

        assertEquals(10, comparison.score)
        assertFalse(comparison.shouldLog)
        assertFalse(comparison.breached)
        assertEquals(listOf("hostname_stem-mismatch"), comparison.reasons)
    }

    @Test
    fun `strong plus medium mismatch breaches when overlap is enforceable`() {
        val comparison = service.compare(acceptedEnforceableFingerprint(), breachedFingerprint())

        assertEquals(90, comparison.score)
        assertTrue(comparison.shouldLog)
        assertTrue(comparison.breached)
        assertEquals(
            listOf("dhcp_prl_hash-mismatch", "dhcp_vendor_class-mismatch"),
            comparison.reasons,
        )
    }

    @Test
    fun `partial accepted profile never breaches`() {
        val accepted = partialFingerprint()
        val current =
            profile(
                DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS to
                    signal("android-dhcp-15", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.MEDIUM),
            )

        val comparison = service.compare(accepted, current)

        assertEquals(DeviceFingerprintStatus.PARTIAL, comparison.status)
        assertEquals(35, comparison.score)
        assertFalse(comparison.shouldLog)
        assertFalse(comparison.breached)
    }

    @Test
    fun `no overlap adds insufficient evidence reason`() {
        val accepted =
            profile(
                DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to
                    signal("hash-a", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.STRONG),
            )
        val current =
            profile(
                DeviceFingerprintService.SIGNAL_UA_FAMILY_MAJOR to
                    signal("Chrome/136", "http:user-agent", DeviceFingerprintSignalStrength.MEDIUM),
            )

        val comparison = service.compare(accepted, current)

        assertEquals(0, comparison.score)
        assertFalse(comparison.breached)
        assertFalse(comparison.shouldLog)
        assertTrue(comparison.reasons.isEmpty())
        assertEquals(0, comparison.matchedSignals)
    }

    @Test
    fun `missing accepted field is neutral and enrichable`() {
        val accepted =
            profile(
                DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to
                    signal("hash-a", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.STRONG),
            )
        val current =
            profile(
                DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to
                    signal("hash-a", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.STRONG),
                DeviceFingerprintService.SIGNAL_HOSTNAME_STEM to
                    signal("office-laptop", "router-agent:hostname", DeviceFingerprintSignalStrength.WEAK),
            )

        val comparison = service.compare(accepted, current)
        val enriched = service.enrichMissingSignals(accepted, current)

        assertEquals(0, comparison.score)
        assertTrue(comparison.reasons.isEmpty())
        assertNotNull(enriched)
        assertEquals(current.signals.keys, enriched!!.signals.keys)
    }

    @Test
    fun `missing current field is neutral`() {
        val accepted =
            profile(
                DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to
                    signal("hash-a", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.STRONG),
                DeviceFingerprintService.SIGNAL_HOSTNAME_STEM to
                    signal("office-laptop", "router-agent:hostname", DeviceFingerprintSignalStrength.WEAK),
            )
        val current =
            profile(
                DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to
                    signal("hash-a", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.STRONG),
            )

        val comparison = service.compare(accepted, current)

        assertEquals(0, comparison.score)
        assertTrue(comparison.reasons.isEmpty())
        assertFalse(comparison.breached)
    }

    @Test
    fun `toJson and fromJson round trip full profile`() {
        val profile = acceptedEnforceableFingerprint()

        val json = service.toJson(profile)
        val roundTrip = service.fromJson(json)

        assertNotNull(json)
        assertEquals(profile, roundTrip)
    }

    @Test
    fun `sourcesToJson emits source only json`() {
        val json = service.sourcesToJson(acceptedEnforceableFingerprint())

        assertEquals(
            """{"dhcp_prl_hash":"router-agent:dhcp-log","dhcp_vendor_class":"router-agent:dhcp-log"}""",
            json,
        )
    }

    @Test
    fun `null and empty profiles serialize to null`() {
        assertNull(service.toJson(null))
        assertNull(service.sourcesToJson(null))
        assertNull(service.toJson(profile()))
    }

    private fun acceptedEnforceableFingerprint(): DeviceFingerprintProfile =
        profile(
            DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to
                signal("hash-a", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.STRONG),
            DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS to
                signal("android-dhcp-14", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.MEDIUM),
        )

    private fun partialFingerprint(): DeviceFingerprintProfile =
        profile(
            DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS to
                signal("android-dhcp-14", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.MEDIUM),
        )

    private fun breachedFingerprint(): DeviceFingerprintProfile =
        profile(
            DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to
                signal("hash-b", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.STRONG),
            DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS to
                signal("android-dhcp-15", "router-agent:dhcp-log", DeviceFingerprintSignalStrength.MEDIUM),
        )

    private fun profile(vararg entries: Pair<String, DeviceFingerprintSignal>): DeviceFingerprintProfile =
        DeviceFingerprintProfile(signals = linkedMapOf(*entries))

    private fun signal(
        value: String,
        source: String,
        strength: DeviceFingerprintSignalStrength,
    ) = DeviceFingerprintSignal(value = value, source = source, strength = strength)
}
