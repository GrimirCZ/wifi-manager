package cz.grimir.wifimanager.captive.application.event

data class MacAuthorizationStateChangedEvent(
    val macAddresses: Collection<String>,
)