package cz.grimir.wifimanager.captive.core.exceptions

/**
 * Device's access was denied because the code was not found.
 */
class InvalidAccessCodeException(
    val accessCode: String,
    val macAddress: String,
) : RuntimeException()
