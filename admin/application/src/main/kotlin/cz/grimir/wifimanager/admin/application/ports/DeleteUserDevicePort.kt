package cz.grimir.wifimanager.admin.application.ports

import cz.grimir.wifimanager.shared.core.UserId

interface DeleteUserDevicePort {
    fun delete(
        userId: UserId,
        mac: String,
    )
}
