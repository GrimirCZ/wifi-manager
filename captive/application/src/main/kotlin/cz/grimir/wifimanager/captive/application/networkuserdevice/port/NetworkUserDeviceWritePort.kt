package cz.grimir.wifimanager.captive.application.networkuserdevice.port

import cz.grimir.wifimanager.captive.application.networkuserdevice.model.NetworkUserDevice
import cz.grimir.wifimanager.shared.core.UserId

interface NetworkUserDeviceWritePort {
    fun save(device: NetworkUserDevice)

    fun delete(
        userId: UserId,
        mac: String,
    )

    fun touchDevice(
        userId: UserId,
        mac: String,
    )
}
