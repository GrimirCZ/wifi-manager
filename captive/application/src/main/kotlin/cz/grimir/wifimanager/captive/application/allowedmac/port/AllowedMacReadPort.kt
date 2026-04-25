package cz.grimir.wifimanager.captive.application.allowedmac.port

import cz.grimir.wifimanager.captive.application.allowedmac.model.AllowedMac

interface AllowedMacReadPort {
    fun findAll(): List<AllowedMac>

    fun findAllAuthorizedMacs(): List<String>

    fun findByMac(mac: String): AllowedMac?
}
