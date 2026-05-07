package cz.grimir.wifimanager.shared.events

data class NetworkUserDevicesLastSeenObservedEvent(
    val changes: List<NetworkUserDeviceLastSeenChange>,
)
