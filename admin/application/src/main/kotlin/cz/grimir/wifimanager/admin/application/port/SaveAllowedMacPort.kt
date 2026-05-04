package cz.grimir.wifimanager.admin.application.port

import cz.grimir.wifimanager.admin.core.value.AllowedMac

interface SaveAllowedMacPort {
    fun save(allowedMac: AllowedMac)
}
