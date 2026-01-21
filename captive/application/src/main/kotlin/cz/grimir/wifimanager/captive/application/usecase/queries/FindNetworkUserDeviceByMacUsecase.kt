package cz.grimir.wifimanager.captive.application.usecase.queries

import cz.grimir.wifimanager.captive.application.network.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.ports.NetworkUserDeviceReadPort
import org.springframework.stereotype.Service

@Service
class FindNetworkUserDeviceByMacUsecase(
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
) {
    fun find(mac: String): NetworkUserDevice? = networkUserDeviceReadPort.findByMac(mac)
}
