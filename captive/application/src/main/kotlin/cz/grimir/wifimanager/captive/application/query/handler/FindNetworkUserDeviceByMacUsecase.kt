package cz.grimir.wifimanager.captive.application.query.handler

import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.captive.application.query.FindNetworkUserDeviceByMacQuery
import cz.grimir.wifimanager.captive.application.query.model.NetworkUserDevice
import org.springframework.stereotype.Service

@Service
class FindNetworkUserDeviceByMacUsecase(
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
) {
    fun find(query: FindNetworkUserDeviceByMacQuery): NetworkUserDevice? = networkUserDeviceReadPort.findByMac(query.mac)
}
