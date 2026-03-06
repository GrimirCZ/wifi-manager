package cz.grimir.wifimanager.captive.application.allowedmac.port

import cz.grimir.wifimanager.captive.application.allowedmac.model.AllowedMac

interface AllowedMacWritePort {
    fun save(allowedMac: AllowedMac)

    fun deleteByMac(mac: String)
}
