package cz.grimir.wifimanager.captive.application.network

object MacAddressUtils {
    fun isLocallyAdministered(mac: String): Boolean {
        val normalized = mac.replace("-", ":")
        val firstOctet = normalized.split(":").firstOrNull() ?: return false
        return try {
            val value = firstOctet.toInt(16)
            (value and 0b00000010) != 0
        } catch (_: NumberFormatException) {
            false
        }
    }
}
