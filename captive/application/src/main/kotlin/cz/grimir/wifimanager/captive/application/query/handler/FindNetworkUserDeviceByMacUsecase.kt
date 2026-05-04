package cz.grimir.wifimanager.captive.application.query.handler

import cz.grimir.wifimanager.captive.application.query.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceReadPort
import org.springframework.stereotype.Service

@Service
class FindNetworkUserDeviceByMacUsecase(
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
) {
    fun find(mac: String): NetworkUserDevice? = networkUserDeviceReadPort.findByMac(mac)
}
