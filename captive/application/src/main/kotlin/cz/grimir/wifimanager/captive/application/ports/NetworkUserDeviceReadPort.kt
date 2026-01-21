package cz.grimir.wifimanager.captive.application.ports

import cz.grimir.wifimanager.captive.application.network.NetworkUserDevice
import cz.grimir.wifimanager.shared.core.UserId

interface NetworkUserDeviceReadPort {
    fun findByMac(mac: String): NetworkUserDevice?

    fun findByUserId(userId: UserId): List<NetworkUserDevice>

    fun countByUserId(userId: UserId): Long
}
