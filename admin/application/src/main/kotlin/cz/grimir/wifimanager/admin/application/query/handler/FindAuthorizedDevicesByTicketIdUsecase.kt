package cz.grimir.wifimanager.admin.application.query.handler

import cz.grimir.wifimanager.admin.application.port.FindAuthorizedDevicePort
import cz.grimir.wifimanager.admin.application.query.FindAuthorizedDevicesByTicketIdQuery
import cz.grimir.wifimanager.admin.core.value.AuthorizedDevice
import org.springframework.stereotype.Service

@Service
class FindAuthorizedDevicesByTicketIdUsecase(
    private val findAuthorizedDevicePort: FindAuthorizedDevicePort,
) {
    fun find(query: FindAuthorizedDevicesByTicketIdQuery): List<AuthorizedDevice> = findAuthorizedDevicePort.findByTicketId(query.ticketId)
}
