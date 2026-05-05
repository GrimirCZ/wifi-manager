package cz.grimir.wifimanager.admin.application.command

import cz.grimir.wifimanager.admin.core.value.UserDevice

data class UpsertUserDeviceCommand(
    val device: UserDevice,
)
