package cz.grimir.wifimanager.captive.application.ports

import cz.grimir.wifimanager.captive.application.allowed.AllowedMac

interface AllowedMacReadPort {
    fun findAll(): List<AllowedMac>

    fun findAllMacs(): List<String>

    fun findByMac(mac: String): AllowedMac?
}
