package cz.grimir.wifimanager.captive.application.authorization.command

import cz.grimir.wifimanager.captive.core.value.Device

data class AuthorizeDeviceWithCodeCommand(
    val accessCode: String,
    val device: Device,
)
