package cz.grimir.wifimanager.admin.application.usecases.queries

import cz.grimir.wifimanager.admin.application.ports.FindAuthorizedDevicePort
import cz.grimir.wifimanager.admin.application.queries.CountAuthorizedDevicesByTicketIdQuery
import org.springframework.stereotype.Service

@Service
class CountAuthorizedDevicesByTicketIdUsecase(
    private val findAuthorizedDevicePort: FindAuthorizedDevicePort,
) {
    fun count(query: CountAuthorizedDevicesByTicketIdQuery): Long = findAuthorizedDevicePort.countByTicketId(query.ticketId)
}
