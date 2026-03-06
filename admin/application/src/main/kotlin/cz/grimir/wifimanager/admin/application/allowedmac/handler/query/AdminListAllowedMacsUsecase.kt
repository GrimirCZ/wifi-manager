package cz.grimir.wifimanager.admin.application.allowedmac.handler.query

import cz.grimir.wifimanager.admin.application.allowedmac.port.FindAllowedMacPort
import cz.grimir.wifimanager.admin.core.value.AllowedMac
import org.springframework.stereotype.Service

@Service
class AdminListAllowedMacsUsecase(
    private val findAllowedMacPort: FindAllowedMacPort,
) {
    fun list(): List<AllowedMac> = findAllowedMacPort.findAll()
}
