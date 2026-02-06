package cz.grimir.wifimanager.admin.application.usecases.queries

import cz.grimir.wifimanager.admin.application.ports.FindAllowedMacPort
import cz.grimir.wifimanager.admin.core.value.AllowedMac
import org.springframework.stereotype.Service

@Service
class AdminListAllowedMacsUsecase(
    private val findAllowedMacPort: FindAllowedMacPort,
) {
    fun list(): List<AllowedMac> = findAllowedMacPort.findAll()
}
