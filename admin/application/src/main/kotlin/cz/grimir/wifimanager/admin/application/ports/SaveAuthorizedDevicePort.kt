package cz.grimir.wifimanager.admin.application.ports

import cz.grimir.wifimanager.admin.core.value.AuthorizedDevice

interface SaveAuthorizedDevicePort {
    fun save(authorizedDevice: AuthorizedDevice)
}