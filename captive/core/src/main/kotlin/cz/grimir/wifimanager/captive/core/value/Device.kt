package cz.grimir.wifimanager.captive.core.value

data class Device(
    /**
     * Device mac address.
     *
     * Serves as primary device identifier.
     */
    val mac: String,
    /**
     * Person name captured during ticket login, if any.
     */
    val displayName: String?,
    /**
     * Device hostname or another device-provided label, if provided.
     */
    val deviceName: String?,
)
