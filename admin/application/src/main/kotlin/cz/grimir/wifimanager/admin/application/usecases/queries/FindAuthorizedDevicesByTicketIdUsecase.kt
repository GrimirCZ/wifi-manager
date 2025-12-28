package cz.grimir.wifimanager.admin.application.usecases.queries

import cz.grimir.wifimanager.admin.application.ports.FindAuthorizedDevicePort
import cz.grimir.wifimanager.admin.application.queries.FindAuthorizedDevicesByTicketIdQuery
import cz.grimir.wifimanager.admin.core.value.AuthorizedDevice
import org.springframework.stereotype.Service

@Service
class FindAuthorizedDevicesByTicketIdUsecase(
    private val findAuthorizedDevicePort: FindAuthorizedDevicePort,
) {
    fun find(query: FindAuthorizedDevicesByTicketIdQuery): List<AuthorizedDevice> = findAuthorizedDevicePort.findByTicketId(query.ticketId)
}
