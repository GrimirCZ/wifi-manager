package cz.grimir.wifimanager.captive.application.authorization.event

data class MacAuthorizationStateChangedEvent(
    val macAddresses: Collection<String>,
)
