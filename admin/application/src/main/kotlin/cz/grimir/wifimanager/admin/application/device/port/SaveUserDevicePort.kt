package cz.grimir.wifimanager.admin.application.device.port

import cz.grimir.wifimanager.admin.core.value.UserDevice

interface SaveUserDevicePort {
    fun save(device: UserDevice)
}
