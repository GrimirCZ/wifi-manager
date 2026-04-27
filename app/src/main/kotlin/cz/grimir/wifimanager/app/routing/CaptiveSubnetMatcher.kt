package cz.grimir.wifimanager.app.routing

import org.springframework.stereotype.Component
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

@Component
class CaptiveSubnetMatcher(
    properties: CaptiveRequestRoutingProperties,
) {
    private val ipv4Ranges = properties.ipv4Subnets.map { parseCidr(it, Inet4Address::class.java) }
    private val ipv6Ranges = properties.ipv6Subnets.map { parseCidr(it, Inet6Address::class.java) }

    fun matches(ipAddress: String): Boolean =
        when (val address = InetAddress.getByName(ipAddress)) {
            is Inet4Address -> ipv4Ranges.any { it.contains(address) }
            is Inet6Address -> ipv6Ranges.any { it.contains(address) }
            else -> false
        }

    private fun <T : InetAddress> parseCidr(
        cidr: String,
        expectedType: Class<T>,
    ): ParsedCidr {
        val normalized = cidr.trim()
        require(normalized.contains("/")) {
            "Invalid CIDR '$cidr': missing prefix length"
        }

        val (baseAddressRaw, prefixLengthRaw) = normalized.split("/", limit = 2)
        val baseAddress =
            InetAddress.getByName(baseAddressRaw).also {
                require(expectedType.isInstance(it)) {
                    "Invalid CIDR '$cidr': expected ${expectedType.simpleName}"
                }
            }
        val totalBits = baseAddress.address.size * 8
        val prefixLength =
            requireNotNull(prefixLengthRaw.toIntOrNull()) {
                "Invalid CIDR '$cidr': prefix length must be a number"
            }

        require(prefixLength in 0..totalBits) {
            "Invalid CIDR '$cidr': prefix length must be between 0 and $totalBits"
        }

        return ParsedCidr(baseAddress.address, prefixLength)
    }

    private class ParsedCidr(
        private val networkAddress: ByteArray,
        prefixLength: Int,
    ) {
        private val fullBytes = prefixLength / 8
        private val remainingBits = prefixLength % 8
        private val partialMask =
            if (remainingBits == 0) {
                0
            } else {
                0xFF shl (8 - remainingBits) and 0xFF
            }

        fun contains(address: InetAddress): Boolean {
            val candidate = address.address
            if (candidate.size != networkAddress.size) {
                return false
            }
            repeat(fullBytes) { index ->
                if (candidate[index] != networkAddress[index]) {
                    return false
                }
            }
            if (remainingBits == 0) {
                return true
            }

            val index = fullBytes
            return candidate[index].toInt() and partialMask == networkAddress[index].toInt() and partialMask
        }
    }
}
