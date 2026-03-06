package cz.grimir.wifimanager.captive.application.networkuserdevice.handler.query

import cz.grimir.wifimanager.captive.application.networkuserdevice.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.networkuserdevice.port.NetworkUserDeviceReadPort
import org.springframework.stereotype.Service

@Service
class FindNetworkUserDeviceByMacUsecase(
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
) {
    fun find(mac: String): NetworkUserDevice? = networkUserDeviceReadPort.findByMac(mac)
}
