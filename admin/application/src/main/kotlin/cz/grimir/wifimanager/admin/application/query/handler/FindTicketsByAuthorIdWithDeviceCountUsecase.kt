package cz.grimir.wifimanager.admin.application.query.handler

import cz.grimir.wifimanager.admin.application.port.FindTicketPort
import cz.grimir.wifimanager.admin.application.query.FindTicketsByAuthorIdWithDeviceCountQuery
import cz.grimir.wifimanager.admin.application.query.model.TicketWithDeviceCount
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FindTicketsByAuthorIdWithDeviceCountUsecase(
    private val findTicketPort: FindTicketPort,
) {
    fun find(query: FindTicketsByAuthorIdWithDeviceCountQuery): List<TicketWithDeviceCount> =
        findTicketPort.findByAuthorIdWithDeviceCount(query.authorId.id)
}
