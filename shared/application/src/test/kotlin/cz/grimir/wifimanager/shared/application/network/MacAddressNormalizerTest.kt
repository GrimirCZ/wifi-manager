package cz.grimir.wifimanager.shared.application.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MacAddressNormalizerTest {
    @Test
    fun `normalizes mac by trimming lowercasing and replacing dashes`() {
        val normalized = MacAddressNormalizer.normalize("  AA-BB-CC-DD-EE-FF  ")

        assertEquals("aa:bb:cc:dd:ee:ff", normalized)
    }

    @Test
    fun `keeps already normalized mac intact`() {
        val normalized = MacAddressNormalizer.normalize("aa:bb:cc:dd:ee:ff")

        assertEquals("aa:bb:cc:dd:ee:ff", normalized)
    }

    @Test
    fun `keeps blank mac blank after trimming`() {
        val normalized = MacAddressNormalizer.normalize("   ")

        assertEquals("", normalized)
    }
}
