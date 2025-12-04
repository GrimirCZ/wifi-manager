package cz.grimir.wifimanager.captive.core.exceptions

/**
 * Device's access was denied because it was previously kicked by a user.
 */
class KickedAddressAttemptedLoginException(val macAddress: String) : RuntimeException()