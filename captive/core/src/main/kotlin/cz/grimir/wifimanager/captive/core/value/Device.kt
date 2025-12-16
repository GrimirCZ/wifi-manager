package cz.grimir.wifimanager.captive.core.value

data class Device(
    /**
     * Device mac address.
     *
     * Serves as primary device identifier.
     */
    val mac: String,
    /**
     * Device hostname, if provided.
     */
    val name: String?,
)
