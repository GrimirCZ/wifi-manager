package cz.grimir.wifimanager.admin.application.ports

import cz.grimir.wifimanager.admin.core.value.UserDevice
import cz.grimir.wifimanager.shared.core.UserId

interface FindUserDevicePort {
    fun findByUserId(userId: UserId): List<UserDevice>

    fun findByUserIdAndMac(
        userId: UserId,
        mac: String,
    ): UserDevice?

    fun findAll(): List<UserDevice>
}
