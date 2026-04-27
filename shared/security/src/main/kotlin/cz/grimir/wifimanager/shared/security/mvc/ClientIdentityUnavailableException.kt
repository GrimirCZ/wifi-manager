package cz.grimir.wifimanager.shared.security.mvc

class ClientIdentityUnavailableException(
    ip: String,
) : RuntimeException("Client identity is unavailable for $ip")
