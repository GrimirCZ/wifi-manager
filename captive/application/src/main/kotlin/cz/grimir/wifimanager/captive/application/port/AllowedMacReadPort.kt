package cz.grimir.wifimanager.captive.application.port

import cz.grimir.wifimanager.captive.application.query.model.AllowedMac

interface AllowedMacReadPort {
    fun findAll(): List<AllowedMac>

    fun findAllAuthorizedMacs(): List<String>

    fun findByMac(mac: String): AllowedMac?
}
