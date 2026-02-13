package cz.grimir.wifimanager.admin.application.ports

import cz.grimir.wifimanager.admin.core.value.UserDevice

interface SaveUserDevicePort {
    fun save(device: UserDevice)
}
