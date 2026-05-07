package cz.grimir.wifimanager.admin.application.command

import cz.grimir.wifimanager.shared.events.NetworkUserDeviceLastSeenChange

data class UpdateUserDevicesLastSeenCommand(
    val changes: List<NetworkUserDeviceLastSeenChange>,
)
