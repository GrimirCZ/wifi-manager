package cz.grimir.wifimanager.captive.application.ports

import cz.grimir.wifimanager.captive.application.allowed.AllowedMac

interface AllowedMacWritePort {
    fun save(allowedMac: AllowedMac)

    fun deleteByMac(mac: String)
}
