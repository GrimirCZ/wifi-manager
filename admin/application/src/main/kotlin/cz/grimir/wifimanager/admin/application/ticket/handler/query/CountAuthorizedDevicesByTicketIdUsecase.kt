package cz.grimir.wifimanager.admin.application.ticket.handler.query

import cz.grimir.wifimanager.admin.application.ticket.port.FindAuthorizedDevicePort
import cz.grimir.wifimanager.admin.application.ticket.query.CountAuthorizedDevicesByTicketIdQuery
import org.springframework.stereotype.Service

@Service
class CountAuthorizedDevicesByTicketIdUsecase(
    private val findAuthorizedDevicePort: FindAuthorizedDevicePort,
) {
    fun count(query: CountAuthorizedDevicesByTicketIdQuery): Long = findAuthorizedDevicePort.countByTicketId(query.ticketId)
}
