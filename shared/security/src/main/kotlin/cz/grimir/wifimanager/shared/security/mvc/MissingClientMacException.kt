package cz.grimir.wifimanager.shared.security.mvc

class MissingClientMacException(
    val ip: String,
) : RuntimeException("Client MAC address is blank for $ip")
