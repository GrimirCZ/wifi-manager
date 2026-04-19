package cz.grimir.wifimanager.app.routing

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CaptiveSubnetMatcherTest {
    @Test
    fun `matches ipv4 address inside configured subnet`() {
        val matcher =
            CaptiveSubnetMatcher(
                CaptiveRequestRoutingProperties(ipv4Subnets = listOf("10.42.0.0/16")),
            )

        assertTrue(matcher.matches("10.42.12.34"))
        assertFalse(matcher.matches("10.43.12.34"))
    }

    @Test
    fun `matches ipv6 address inside configured subnet`() {
        val matcher =
            CaptiveSubnetMatcher(
                CaptiveRequestRoutingProperties(ipv6Subnets = listOf("2001:db8:42::/64")),
            )

        assertTrue(matcher.matches("2001:db8:42::99"))
        assertFalse(matcher.matches("2001:db8:feed::99"))
    }

    @Test
    fun `empty config does not match any address`() {
        val matcher = CaptiveSubnetMatcher(CaptiveRequestRoutingProperties())

        assertFalse(matcher.matches("192.168.1.10"))
        assertFalse(matcher.matches("2001:db8::1"))
    }

    @Test
    fun `invalid cidr config is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            CaptiveSubnetMatcher(
                CaptiveRequestRoutingProperties(ipv4Subnets = listOf("10.42.0.0")),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            CaptiveSubnetMatcher(
                CaptiveRequestRoutingProperties(ipv6Subnets = listOf("2001:db8::/129")),
            )
        }
    }

    @Test
    fun `ipv4 and ipv6 ranges stay family specific`() {
        assertDoesNotThrow {
            CaptiveSubnetMatcher(
                CaptiveRequestRoutingProperties(
                    ipv4Subnets = listOf("10.42.0.0/16"),
                    ipv6Subnets = listOf("2001:db8::/64"),
                ),
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            CaptiveSubnetMatcher(
                CaptiveRequestRoutingProperties(ipv4Subnets = listOf("2001:db8::/64")),
            )
        }
    }
}
