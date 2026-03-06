package cz.grimir.wifimanager.admin.application.device.port

import cz.grimir.wifimanager.shared.core.UserId

interface DeleteUserDevicePort {
    fun delete(
        userId: UserId,
        mac: String,
    )
}
