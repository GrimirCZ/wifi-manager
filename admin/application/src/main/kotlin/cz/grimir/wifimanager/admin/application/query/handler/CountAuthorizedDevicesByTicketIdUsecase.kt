package cz.grimir.wifimanager.admin.application.query.handler

import cz.grimir.wifimanager.admin.application.port.FindAuthorizedDevicePort
import cz.grimir.wifimanager.admin.application.query.CountAuthorizedDevicesByTicketIdQuery
import org.springframework.stereotype.Service

@Service
class CountAuthorizedDevicesByTicketIdUsecase(
    private val findAuthorizedDevicePort: FindAuthorizedDevicePort,
) {
    fun count(query: CountAuthorizedDevicesByTicketIdQuery): Long = findAuthorizedDevicePort.countByTicketId(query.ticketId)
}
