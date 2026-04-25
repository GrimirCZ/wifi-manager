package cz.grimir.wifimanager.captive.application.networkuserdevice.port

import cz.grimir.wifimanager.captive.application.networkuserdevice.model.NetworkUserDevice
import cz.grimir.wifimanager.shared.core.UserId

interface NetworkUserDeviceReadPort {
    fun findByMac(mac: String): NetworkUserDevice?

    fun findAllAuthorizedMacs(): List<String>

    fun findByUserId(userId: UserId): List<NetworkUserDevice>

    fun countByUserId(userId: UserId): Long
}
