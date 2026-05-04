package cz.grimir.wifimanager.captive.application.port

import cz.grimir.wifimanager.captive.application.query.model.AllowedMac

interface AllowedMacWritePort {
    fun save(allowedMac: AllowedMac)

    fun deleteByMac(mac: String)
}
