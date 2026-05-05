package cz.grimir.wifimanager.captive.application.command

data class ScrubDeauthorizedCaptiveDeviceCommand(
    val macAddress: String,
)
