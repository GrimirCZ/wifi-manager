package cz.grimir.wifimanager.admin.application.ports

import cz.grimir.wifimanager.admin.core.value.AllowedMac

interface SaveAllowedMacPort {
    fun save(allowedMac: AllowedMac)
}
